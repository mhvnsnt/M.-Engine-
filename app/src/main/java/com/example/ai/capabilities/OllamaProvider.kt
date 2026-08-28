package com.example.ai.capabilities

import com.example.ai.PermissionLevel
import com.example.network.OllamaChatRequest
import com.example.network.OllamaMessage
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

class OllamaProvider : ModelProvider {
    override val name = "Ollama"
    override val providerId = "OLLAMA"
    override val type = CapabilityType.MODEL
    override val isLocal = true // Can run on localhost or local network
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream", "tools")
    override val networkRequired = false

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = false,
        supportsTools = true,
        supportsJsonSchema = true,
        contextWindowLength = 32768,
        maxOutputTokens = 4096,
        speedTier = SpeedTier.BALANCED,
        costTier = CostTier.FREE_OR_LOCAL
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (config.url.isBlank()) {
            return@withContext ProviderHealthStatus(
                status = ProviderStatus.UNCONFIGURED,
                latencyMs = 0L,
                message = "Ollama endpoint URL is empty."
            )
        }

        try {
            val tagsUrl = normalizeOllamaUrl(config.url, "/api/tags")
            val request = Request.Builder().url(tagsUrl).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    ProviderHealthStatus(
                        status = ProviderStatus.ONLINE,
                        latencyMs = latency,
                        message = "Ollama daemon reachable ($latency ms)"
                    )
                } else {
                    val errorInfo = ProviderErrorClassifier.classify(null, response.code, response.body?.string())
                    ProviderHealthStatus(
                        status = ProviderStatus.DEGRADED,
                        latencyMs = latency,
                        message = errorInfo.rawMessage
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
        val ollamaHistory = buildHistory(request)
        val chatUrl = normalizeOllamaUrl(request.endpointConfig.url, "/api/chat")
        
        val model = if (request.endpointConfig.modelName.isNotBlank()) request.endpointConfig.modelName else "llama3"
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("stream", false)
            val msgs = JSONArray()
            ollamaHistory.forEach { m ->
                msgs.put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
            put("messages", msgs)
        }

        val httpRequest = Request.Builder()
            .url(chatUrl)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(httpRequest).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorInfo = ProviderErrorClassifier.classify(null, response.code, bodyString)
                throw Exception("Ollama Error [${response.code}]: ${errorInfo.rawMessage}")
            }

            val json = JSONObject(bodyString)
            val msg = json.optJSONObject("message")
            val content = msg?.optString("content").orEmpty()

            ModelResponse(
                text = content,
                modelUsed = model,
                providerUsed = "Ollama",
                latencyMs = latency,
                finishReason = if (json.optBoolean("done", false)) "stop" else null
            )
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val ollamaHistory = buildHistory(request)
        val chatUrl = normalizeOllamaUrl(request.endpointConfig.url, "/api/chat")
        val model = if (request.endpointConfig.modelName.isNotBlank()) request.endpointConfig.modelName else "llama3"
        
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("stream", true)
            val msgs = JSONArray()
            ollamaHistory.forEach { m ->
                msgs.put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
            put("messages", msgs)
        }

        val httpRequest = Request.Builder()
            .url(chatUrl)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            val errorInfo = ProviderErrorClassifier.classify(null, response.code, errorBody)
            response.close()
            throw Exception("Ollama Stream Error [${response.code}]: ${errorInfo.rawMessage}")
        }

        response.body?.byteStream()?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val jsonLine = line?.trim().orEmpty()
                if (jsonLine.isNotBlank()) {
                    try {
                        val json = JSONObject(jsonLine)
                        val msg = json.optJSONObject("message")
                        val content = msg?.optString("content")
                        if (!content.isNullOrEmpty()) {
                            emit(ModelStream(chunk = content, providerUsed = "Ollama"))
                        }
                    } catch (e: Exception) {
                        // ignore malformed NDJSON lines
                    }
                }
            }
        }
        emit(ModelStream(chunk = "", providerUsed = "Ollama", isComplete = true))
    }

    private fun normalizeOllamaUrl(rawUrl: String, path: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith(path) -> trimmed
            trimmed.endsWith("/api/chat") && path == "/api/tags" -> trimmed.removeSuffix("/api/chat") + "/api/tags"
            trimmed.endsWith("/api/tags") && path == "/api/chat" -> trimmed.removeSuffix("/api/tags") + "/api/chat"
            else -> "$trimmed$path"
        }
    }

    private fun buildHistory(request: ModelRequest): List<OllamaMessage> {
        val ollamaHistory = mutableListOf<OllamaMessage>()
        if (request.systemPrompt.isNotBlank()) {
            ollamaHistory.add(OllamaMessage(role = "system", content = request.systemPrompt))
        }
        request.messages.forEach { msg ->
            ollamaHistory.add(OllamaMessage(role = msg.role, content = msg.content))
        }
        return ollamaHistory
    }
}
