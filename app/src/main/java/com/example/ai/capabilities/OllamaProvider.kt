package com.example.ai.capabilities

import com.example.ai.PermissionLevel
import com.example.network.OllamaChatRequest
import com.example.network.OllamaMessage
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStreamReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OllamaProvider : ModelProvider {
    override val name = "Ollama"
    override val type = CapabilityType.MODEL
    override val isLocal = false // Could be local network or remote
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream")
    override val networkRequired = true

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = false,
        supportsTools = true,
        contextWindowLength = 8192
    )

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val ollamaHistory = buildHistory(request)
        val req = OllamaChatRequest(
            model = request.endpointConfig.modelName,
            messages = ollamaHistory,
            stream = false
        )
        val response = RetrofitClient.service.generateChatStream(request.endpointConfig.url, req)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        
        return withContext(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OllamaChatResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    try {
                        val chunk = adapter.fromJson(jsonLine)
                        chunk?.message?.content?.let { completeResponse += it }
                    } catch (e: Exception) {}
                }
            }
            ModelResponse(completeResponse)
        }
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val ollamaHistory = buildHistory(request)
        val req = OllamaChatRequest(
            model = request.endpointConfig.modelName,
            messages = ollamaHistory,
            stream = true
        )
        val response = RetrofitClient.service.generateChatStream(request.endpointConfig.url, req)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }
        
        val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
        var line: String?
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OllamaChatResponse::class.java)
        
        while (reader.readLine().also { line = it } != null) {
            line?.let { jsonLine ->
                try {
                    val chunk = adapter.fromJson(jsonLine)
                    chunk?.message?.content?.let { 
                        emit(ModelStream(it))
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun buildHistory(request: ModelRequest): List<OllamaMessage> {
        val ollamaHistory = mutableListOf<OllamaMessage>()
        ollamaHistory.add(OllamaMessage(role = "system", content = request.systemPrompt))
        request.messages.forEach { msg ->
            ollamaHistory.add(OllamaMessage(role = msg.role, content = msg.content))
        }
        return ollamaHistory
    }
}
