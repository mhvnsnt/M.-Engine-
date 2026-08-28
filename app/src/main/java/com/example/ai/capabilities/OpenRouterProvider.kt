package com.example.ai.capabilities

import com.example.ai.PermissionLevel
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterImageUrl
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterRequest
import com.example.network.RetrofitClient
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

class OpenRouterProvider : ModelProvider {
    override val name = "OpenRouter"
    override val providerId = "OPENROUTER"
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
        contextWindowLength = 200000,
        maxOutputTokens = 8192,
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
        if (config.apiKey.isBlank()) {
            return@withContext ProviderHealthStatus(
                status = ProviderStatus.UNCONFIGURED,
                latencyMs = 0L,
                message = "OpenRouter API key is not configured."
            )
        }

        try {
            val url = if (config.url.isNotBlank()) config.url else "https://openrouter.ai/api/v1/chat/completions"
            val payload = JSONObject().apply {
                put("model", if (config.modelName.isNotBlank()) config.modelName else "openai/gpt-4o-mini")
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                }))
                put("max_tokens", 5)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("HTTP-Referer", "https://mengine.ai")
                .addHeader("X-Title", "M. Engine")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ProviderHealthStatus(
                        status = ProviderStatus.ONLINE,
                        latencyMs = latency,
                        message = "OpenRouter responsive ($latency ms)"
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
        val url = if (request.endpointConfig.url.isNotBlank()) request.endpointConfig.url else "https://openrouter.ai/api/v1/chat/completions"
        val payload = buildOpenRouterPayload(request, stream = false)

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${request.endpointConfig.apiKey}")
            .addHeader("HTTP-Referer", "https://mengine.ai")
            .addHeader("X-Title", "M. Engine")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorInfo = ProviderErrorClassifier.classify(null, response.code, bodyString)
                throw Exception("OpenRouter API Error [${response.code}]: ${errorInfo.rawMessage}")
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
                providerUsed = "OpenRouter",
                latencyMs = latency,
                finishReason = firstChoice?.optString("finish_reason"),
                toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
            )
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val url = if (request.endpointConfig.url.isNotBlank()) request.endpointConfig.url else "https://openrouter.ai/api/v1/chat/completions"
        val payload = buildOpenRouterPayload(request, stream = true)

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${request.endpointConfig.apiKey}")
            .addHeader("HTTP-Referer", "https://mengine.ai")
            .addHeader("X-Title", "M. Engine")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorInfo = ProviderErrorClassifier.classify(null, response.code, errorBody)
            response.close()
            throw Exception("OpenRouter Stream Error [${response.code}]: ${errorInfo.rawMessage}")
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
                                emit(ModelStream(chunk = chunkText, providerUsed = "OpenRouter"))
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        }
        emit(ModelStream(chunk = "", providerUsed = "OpenRouter", isComplete = true))
    }

    private fun buildOpenRouterPayload(request: ModelRequest, stream: Boolean): JSONObject {
        val root = JSONObject()
        val model = if (request.endpointConfig.modelName.isNotBlank()) request.endpointConfig.modelName else "openai/gpt-4o"
        root.put("model", model)
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
