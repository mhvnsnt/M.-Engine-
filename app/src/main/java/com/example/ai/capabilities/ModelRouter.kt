package com.example.ai.capabilities

import com.example.data.EndpointEntity
import kotlinx.coroutines.flow.Flow

class ModelRouter(private val capabilityRegistry: CapabilityRegistry) {

    private fun getProviderForEndpoint(endpoint: EndpointEntity): ModelProvider {
        val providers = capabilityRegistry.getProviders(CapabilityType.MODEL)
        return when (endpoint.type) {
            "OPENROUTER", "OPENAI", "GROQ" -> {
                (providers.find { it is OpenRouterProvider } as? ModelProvider) ?: OpenRouterProvider()
            }
            "OLLAMA" -> {
                (providers.find { it is OllamaProvider } as? ModelProvider) ?: OllamaProvider()
            }
            else -> {
                (providers.find { it is OpenRouterProvider } as? ModelProvider) ?: OpenRouterProvider()
            }
        }
    }

    suspend fun generate(endpoints: List<EndpointEntity>, systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        var lastException: Exception? = null
        for (endpoint in endpoints) {
            try {
                val provider = getProviderForEndpoint(endpoint)
                val request = ModelRequest(
                    systemPrompt = systemPrompt,
                    messages = messages,
                    endpointConfig = object : EndpointConfig {
                        override val url = endpoint.url
                        override val apiKey = endpoint.apiKey
                        override val modelName = endpoint.modelName
                    }
                )
                return provider.generate(request)
            } catch (e: Exception) {
                lastException = e
                // log failure and continue to next endpoint
                android.util.Log.e("ModelRouter", "Provider failed for endpoint ${endpoint.url}: ${e.message}")
            }
        }
        throw lastException ?: Exception("No endpoints available")
    }

    suspend fun stream(endpoints: List<EndpointEntity>, systemPrompt: String, messages: List<ModelMessage>): Flow<ModelStream> {
        var lastException: Exception? = null
        for (endpoint in endpoints) {
            try {
                val provider = getProviderForEndpoint(endpoint)
                val request = ModelRequest(
                    systemPrompt = systemPrompt,
                    messages = messages,
                    endpointConfig = object : EndpointConfig {
                        override val url = endpoint.url
                        override val apiKey = endpoint.apiKey
                        override val modelName = endpoint.modelName
                    }
                )
                // Note: since stream() returns a Flow, the actual network call and potential failure 
                // might happen during flow collection. To make fallback work seamlessly with streaming, 
                // we should either handle it inside a custom flow or let the caller handle it.
                // But for now, returning the flow. Real fallback for streaming is complex.
                // The ChatViewModel currently handles fallback for streaming by catching errors in the try-catch block 
                // and returning false from streamOpenRouterModel.
                return provider.stream(request)
            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("ModelRouter", "Provider failed for endpoint ${endpoint.url}: ${e.message}")
            }
        }
        throw lastException ?: Exception("No endpoints available")
    }
}

