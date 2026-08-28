package com.example.ai.capabilities

import com.example.ai.PermissionLevel
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

class OpenAiCompatibleProvider(
    override val name: String = "OpenAI Compatible",
    override val providerId: String = "OPENAI"
) : ModelProvider {
    override val type = CapabilityType.MODEL
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream", "tools", "vision")
    override val networkRequired = true

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = true,
        supportsTools = true,
        supportsJsonSchema = true,
        contextWindowLength = 131072, // 128k
        maxOutputTokens = 4096,
        speedTier = SpeedTier.BALANCED,
        costTier = CostTier.MEDIUM
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (config.url.isBlank()) {
            return@withContext ProviderHealthStatus(
                status = ProviderStatus.UNCONFIGURED,
                latencyMs = 0L,
                message = "Endpoint URL is empty."
            )
        }

        try {
            val url = normalizeCompletionsUrl(config.url)
            val jsonPayload = JSONObject().apply {
                put("model", if (config.modelName.isNotBlank()) config.modelName else "gpt-4o-mini")
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                }))
                put("max_tokens", 5)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))

            if (config.apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ProviderHealthStatus(
                        status = ProviderStatus.ONLINE,
                        latencyMs = latency,
                        message = "Online (${latency}ms)"
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
        val url = normalizeCompletionsUrl(request.endpointConfig.url)
        val payload = buildOpenAiPayload(request, stream = false)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))

        if (request.endpointConfig.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${request.endpointConfig.apiKey}")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorInfo = ProviderErrorClassifier.classify(null, response.code, bodyString)
                throw Exception("OpenAI API Error [${response.code}]: ${errorInfo.rawMessage}")
            }

            val json = JSONObject(bodyString)
            val choices = json.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content").orEmpty()

            val toolCalls = mutableListOf<ModelToolCall>()
            val rawTools = message?.optJSONArray("tool_calls")
            if (rawTools != null) {
                for (i in 0 until rawTools.length()) {
                    val tc = rawTools.getJSONObject(i)
                    val fn = tc.optJSONObject("function")
                    toolCalls.add(
                        ModelToolCall(
                            id = tc.optString("id", "call-$i"),
                            functionName = fn?.optString("name").orEmpty(),
                            argumentsJson = fn?.optString("arguments") ?: "{}"
                        )
                    )
                }
            }

            ModelResponse(
                text = content,
                modelUsed = json.optString("model", request.endpointConfig.modelName),
                providerUsed = name,
                latencyMs = latency,
                finishReason = firstChoice?.optString("finish_reason"),
                toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
            )
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val url = normalizeCompletionsUrl(request.endpointConfig.url)
        val payload = buildOpenAiPayload(request, stream = true)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))

        if (request.endpointConfig.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${request.endpointConfig.apiKey}")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorInfo = ProviderErrorClassifier.classify(null, response.code, errorBody)
            response.close()
            throw Exception("OpenAI Stream Error [${response.code}]: ${errorInfo.rawMessage}")
        }

        response.body?.byteStream()?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val data = currentLine.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        break
                    }
                    if (data.isNotBlank()) {
                        try {
                            val json = JSONObject(data)
                            val choices = json.optJSONArray("choices")
                            val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
                            val chunkText = delta?.optString("content")
                            if (!chunkText.isNullOrEmpty()) {
                                emit(ModelStream(chunk = chunkText, providerUsed = name))
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        }
        emit(ModelStream(chunk = "", providerUsed = name, isComplete = true))
    }

    private fun normalizeCompletionsUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/v1/chat/completions") -> trimmed
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            trimmed.isNotBlank() -> "$trimmed/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun buildOpenAiPayload(request: ModelRequest, stream: Boolean): JSONObject {
        val root = JSONObject()
        root.put("model", if (request.endpointConfig.modelName.isNotBlank()) request.endpointConfig.modelName else "gpt-4o")
        root.put("stream", stream)

        val messagesArray = JSONArray()

        if (request.systemPrompt.isNotBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", request.systemPrompt)
            })
        }

        request.messages.forEach { msg ->
            if (msg.imageBase64 != null) {
                val contentArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", msg.content)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            val mime = msg.imageMimeType ?: "image/jpeg"
                            put("url", "data:$mime;base64,${msg.imageBase64}")
                        })
                    })
                }
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", contentArray)
                })
            } else {
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        root.put("messages", messagesArray)

        request.temperature?.let { root.put("temperature", it) }
        request.maxTokens?.let { root.put("max_tokens", it) }
        if (request.responseFormatJson) {
            root.put("response_format", JSONObject().put("type", "json_object"))
        }

        if (!request.tools.isNullOrEmpty()) {
            val toolsArray = JSONArray()
            request.tools.forEach { tool ->
                toolsArray.put(JSONObject().apply {
                    put("type", "function")
                    put("function", JSONObject().apply {
                        put("name", tool.name)
                        put("description", tool.description)
                        try {
                            put("parameters", JSONObject(tool.parametersJsonSchema))
                        } catch (e: Exception) {
                            put("parameters", JSONObject())
                        }
                    })
                })
            }
            root.put("tools", toolsArray)
        }

        return root
    }
}
