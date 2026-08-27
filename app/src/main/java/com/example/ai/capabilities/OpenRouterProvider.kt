package com.example.ai.capabilities

import com.example.ai.PermissionLevel
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterRequest
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStreamReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenRouterProvider : ModelProvider {
    override val name = "OpenRouter"
    override val type = CapabilityType.MODEL
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream")
    override val networkRequired = true

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = true,
        supportsTools = true,
        contextWindowLength = 128000 // A default estimation
    )

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val openRouterHistory = buildHistory(request)
        val req = OpenRouterRequest(
            model = request.endpointConfig.modelName,
            messages = openRouterHistory,
            stream = false
        )
        val response = RetrofitClient.openRouterService.generateChatStream(
            url = request.endpointConfig.url, 
            authHeader = "Bearer ${request.endpointConfig.apiKey}", 
            request = req
        )
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        
        return withContext(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OpenRouterResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    if (jsonLine.startsWith("data: ")) {
                        val data = jsonLine.substring(6)
                        if (data != "[DONE]") {
                            try {
                                val chunk = adapter.fromJson(data)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { completeResponse += it }
                            } catch (e: Exception) {}
                        }
                    } else if (jsonLine.startsWith("{")) {
                        try {
                            val resp = adapter.fromJson(jsonLine)
                            val c = resp?.choices?.firstOrNull()
                            (c?.delta?.content ?: c?.message?.content)?.let { completeResponse += it }
                        } catch (e: Exception) {}
                    }
                }
            }
            ModelResponse(completeResponse)
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val openRouterHistory = buildHistory(request)
        val req = OpenRouterRequest(
            model = request.endpointConfig.modelName,
            messages = openRouterHistory,
            stream = true
        )
        val response = RetrofitClient.openRouterService.generateChatStream(
            url = request.endpointConfig.url, 
            authHeader = "Bearer ${request.endpointConfig.apiKey}", 
            request = req
        )
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        
        val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
        var line: String?
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OpenRouterResponse::class.java)
        
        while (reader.readLine().also { line = it } != null) {
            line?.let { jsonLine ->
                if (jsonLine.startsWith("data: ")) {
                    val data = jsonLine.substring(6)
                    if (data != "[DONE]") {
                        try {
                            val chunk = adapter.fromJson(data)
                            chunk?.choices?.firstOrNull()?.delta?.content?.let {
                                emit(ModelStream(it))
                            }
                        } catch (e: Exception) {}
                    }
                } else if (jsonLine.startsWith("{")) {
                    try {
                        val resp = adapter.fromJson(jsonLine)
                        val c = resp?.choices?.firstOrNull()
                        (c?.delta?.content ?: c?.message?.content)?.let { 
                            emit(ModelStream(it))
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private fun buildHistory(request: ModelRequest): List<OpenRouterMessage> {
        val openRouterHistory = mutableListOf<OpenRouterMessage>()
        openRouterHistory.add(OpenRouterMessage(role = "system", content = listOf(OpenRouterContentPart(type = "text", text = request.systemPrompt))))
        request.messages.forEach { msg ->
            openRouterHistory.add(OpenRouterMessage(role = msg.role, content = listOf(OpenRouterContentPart(type = "text", text = msg.content))))
        }
        return openRouterHistory
    }
}
