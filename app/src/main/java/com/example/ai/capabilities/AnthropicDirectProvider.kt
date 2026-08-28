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

class AnthropicDirectProvider : ModelProvider {
    override val name = "Anthropic Claude"
    override val providerId = "ANTHROPIC"
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
        contextWindowLength = 200000, // 200k tokens
        maxOutputTokens = 8192,
        speedTier = SpeedTier.REASONING_HEAVY,
        costTier = CostTier.HIGH
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
                message = "Anthropic API key is not configured."
            )
        }

        try {
            val url = "https://api.anthropic.com/v1/messages"
            val payload = JSONObject().apply {
                put("model", if (config.modelName.isNotBlank()) config.modelName else "claude-3-5-haiku-latest")
                put("max_tokens", 5)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                }))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", config.apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ProviderHealthStatus(
                        status = ProviderStatus.ONLINE,
                        latencyMs = latency,
                        message = "Claude responsive ($latency ms)"
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
        if (request.endpointConfig.apiKey.isBlank()) {
            throw IllegalStateException("Anthropic API key is not configured.")
        }

        val url = "https://api.anthropic.com/v1/messages"
        val payload = buildAnthropicPayload(request, stream = false)

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("x-api-key", request.endpointConfig.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorInfo = ProviderErrorClassifier.classify(null, response.code, bodyString)
                throw Exception("Anthropic API Error [${response.code}]: ${errorInfo.rawMessage}")
            }

            val json = JSONObject(bodyString)
            val contents = json.optJSONArray("content")
            var textResult = ""
            val toolCalls = mutableListOf<ModelToolCall>()

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val block = contents.getJSONObject(i)
                    when (block.optString("type")) {
                        "text" -> textResult += block.optString("text")
                        "tool_use" -> {
                            toolCalls.add(
                                ModelToolCall(
                                    id = block.optString("id", "call-$i"),
                                    functionName = block.optString("name"),
                                    argumentsJson = block.optJSONObject("input")?.toString() ?: "{}"
                                )
                            )
                        }
                    }
                }
            }

            ModelResponse(
                text = textResult,
                modelUsed = json.optString("model", request.endpointConfig.modelName),
                providerUsed = "Anthropic",
                latencyMs = latency,
                finishReason = json.optString("stop_reason"),
                toolCalls = if (toolCalls.isNotEmpty()) toolCalls else null
            )
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        if (request.endpointConfig.apiKey.isBlank()) {
            throw IllegalStateException("Anthropic API key is not configured.")
        }

        val url = "https://api.anthropic.com/v1/messages"
        val payload = buildAnthropicPayload(request, stream = true)

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("x-api-key", request.endpointConfig.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorInfo = ProviderErrorClassifier.classify(null, response.code, errorBody)
            response.close()
            throw Exception("Anthropic Stream Error [${response.code}]: ${errorInfo.rawMessage}")
        }

        response.body?.byteStream()?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val data = currentLine.removePrefix("data:").trim()
                    if (data.isNotBlank() && data != "[DONE]") {
                        try {
                            val json = JSONObject(data)
                            val type = json.optString("type")
                            if (type == "content_block_delta") {
                                val delta = json.optJSONObject("delta")
                                if (delta?.optString("type") == "text_delta") {
                                    val text = delta.optString("text")
                                    if (text.isNotEmpty()) {
                                        emit(ModelStream(chunk = text, providerUsed = "Anthropic"))
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
        emit(ModelStream(chunk = "", providerUsed = "Anthropic", isComplete = true))
    }

    private fun buildAnthropicPayload(request: ModelRequest, stream: Boolean): JSONObject {
        val root = JSONObject()
        val model = if (request.endpointConfig.modelName.isNotBlank()) request.endpointConfig.modelName else "claude-3-5-sonnet-latest"
        root.put("model", model)
        root.put("max_tokens", request.maxTokens ?: 4096)
        root.put("stream", stream)

        if (request.systemPrompt.isNotBlank()) {
            root.put("system", request.systemPrompt)
        }

        request.temperature?.let { root.put("temperature", it) }

        val messagesArray = JSONArray()
        request.messages.forEach { msg ->
            if (msg.role != "system") {
                if (msg.imageBase64 != null) {
                    val contentArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", msg.imageMimeType ?: "image/jpeg")
                                put("data", msg.imageBase64)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", msg.content)
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
        }
        root.put("messages", messagesArray)

        if (!request.tools.isNullOrEmpty()) {
            val toolsArray = JSONArray()
            request.tools.forEach { tool ->
                toolsArray.put(JSONObject().apply {
                    put("name", tool.name)
                    put("description", tool.description)
                    try {
                        put("input_schema", JSONObject(tool.parametersJsonSchema))
                    } catch (e: Exception) {
                        put("input_schema", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject())
                        })
                    }
                })
            }
            root.put("tools", toolsArray)
        }

        return root
    }
}
