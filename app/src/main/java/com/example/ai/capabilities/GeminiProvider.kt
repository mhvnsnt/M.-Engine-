package com.example.ai.capabilities

import com.example.BuildConfig
import com.example.ai.PermissionLevel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiProvider : ModelProvider {
    override val name = "Gemini"
    override val providerId = "GEMINI"
    override val type = CapabilityType.MODEL
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream", "vision", "tools", "thinking")
    override val networkRequired = true

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = true,
        supportsTools = true,
        supportsJsonSchema = true,
        contextWindowLength = 1048576, // 1 Million tokens
        maxOutputTokens = 8192,
        speedTier = SpeedTier.FAST,
        costTier = CostTier.LOW
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = resolveApiKey(config)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ProviderHealthStatus(
                status = ProviderStatus.UNCONFIGURED,
                latencyMs = 0L,
                message = "Gemini API key is unconfigured or placeholder."
            )
        }

        try {
            val model = if (config.modelName.isNotBlank()) config.modelName else "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", "ping")))
                }))
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ProviderHealthStatus(
                        status = ProviderStatus.ONLINE,
                        latencyMs = latency,
                        message = "Gemini $model responsive ($latency ms)"
                    )
                } else {
                    val errorInfo = ProviderErrorClassifier.classify(null, response.code, body)
                    val providerStatus = when (errorInfo.kind) {
                        ProviderErrorKind.RATE_LIMITED -> ProviderStatus.RATE_LIMITED
                        ProviderErrorKind.QUOTA_EXHAUSTED -> ProviderStatus.QUOTA_EXHAUSTED
                        ProviderErrorKind.AUTH_FAILED -> ProviderStatus.AUTH_FAILED
                        else -> ProviderStatus.DEGRADED
                    }
                    ProviderHealthStatus(
                        status = providerStatus,
                        latencyMs = latency,
                        message = "HTTP ${response.code}: ${errorInfo.rawMessage}"
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val errorInfo = ProviderErrorClassifier.classify(e)
            ProviderHealthStatus(
                status = ProviderStatus.OFFLINE,
                latencyMs = latency,
                message = errorInfo.rawMessage
            )
        }
    }

    override suspend fun generate(request: ModelRequest): ModelResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = resolveApiKey(request.endpointConfig)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Configure GEMINI_API_KEY in Secrets or Endpoint settings.")
        }

        val model = resolveModelName(request.endpointConfig.modelName)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val payload = buildGeminiPayload(request)

        val httpRequest = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorInfo = ProviderErrorClassifier.classify(null, response.code, bodyString)
                throw Exception("Gemini API Error [${response.code}]: ${errorInfo.rawMessage}")
            }

            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var responseText = ""
            val toolCalls = mutableListOf<ModelToolCall>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        responseText += part.getString("text")
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        toolCalls.add(
                            ModelToolCall(
                                id = "call-${System.currentTimeMillis()}-$i",
                                functionName = fn.optString("name"),
                                argumentsJson = fn.optJSONObject("args")?.toString() ?: "{}"
                            )
                        )
                    }
                }
            }

            ModelResponse(
                text = responseText,
                modelUsed = model,
                providerUsed = "Gemini",
                latencyMs = latency,
                finishReason = candidate?.optString("finishReason"),
                toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
            )
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val apiKey = resolveApiKey(request.endpointConfig)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured.")
        }

        val model = resolveModelName(request.endpointConfig.modelName)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val payload = buildGeminiPayload(request)

        val httpRequest = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorInfo = ProviderErrorClassifier.classify(null, response.code, errorBody)
            response.close()
            throw Exception("Gemini Stream Error [${response.code}]: ${errorInfo.rawMessage}")
        }

        response.body?.byteStream()?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val jsonStr = currentLine.removePrefix("data:").trim()
                    if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                        try {
                            val json = JSONObject(jsonStr)
                            val candidates = json.optJSONArray("candidates")
                            val candidate = candidates?.optJSONObject(0)
                            val content = candidate?.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    if (part.has("text")) {
                                        emit(ModelStream(chunk = part.getString("text"), providerUsed = "Gemini"))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore malformed SSE frames
                        }
                    }
                }
            }
        }
        emit(ModelStream(chunk = "", providerUsed = "Gemini", isComplete = true))
    }

    private fun resolveApiKey(config: EndpointConfig): String {
        return when {
            config.apiKey.isNotBlank() && config.apiKey != "MY_GEMINI_API_KEY" -> config.apiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }
    }

    private fun resolveModelName(specifiedModel: String): String {
        if (specifiedModel.isNotBlank()) return specifiedModel
        return "gemini-3.5-flash"
    }

    private fun buildGeminiPayload(request: ModelRequest): JSONObject {
        val root = JSONObject()

        // System instruction
        if (request.systemPrompt.isNotBlank()) {
            root.put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", request.systemPrompt)))
            })
        }

        // Contents
        val contentsArray = JSONArray()
        request.messages.forEach { msg ->
            val partsArray = JSONArray()
            if (msg.content.isNotBlank()) {
                partsArray.put(JSONObject().put("text", msg.content))
            }
            if (msg.imageBase64 != null) {
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", msg.imageMimeType ?: "image/jpeg")
                        put("data", msg.imageBase64)
                    })
                })
            }
            if (partsArray.length() > 0) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    put("parts", partsArray)
                })
            }
        }
        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        request.temperature?.let { genConfig.put("temperature", it) }
        request.maxTokens?.let { genConfig.put("maxOutputTokens", it) }
        if (request.responseFormatJson) {
            genConfig.put("responseMimeType", "application/json")
        }
        if (genConfig.length() > 0) {
            root.put("generationConfig", genConfig)
        }

        // Tools (Function Calling)
        if (!request.tools.isNullOrEmpty()) {
            val functionDeclarations = JSONArray()
            request.tools.forEach { tool ->
                functionDeclarations.put(JSONObject().apply {
                    put("name", tool.name)
                    put("description", tool.description)
                    try {
                        put("parameters", JSONObject(tool.parametersJsonSchema))
                    } catch (e: Exception) {
                        put("parameters", JSONObject())
                    }
                })
            }
            root.put("tools", JSONArray().put(JSONObject().apply {
                put("functionDeclarations", functionDeclarations)
            }))
        }

        return root
    }
}
