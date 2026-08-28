package com.example.ai.capabilities

import kotlinx.coroutines.flow.Flow

data class ModelToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String = "{}"
)

data class ModelToolCall(
    val id: String,
    val functionName: String,
    val argumentsJson: String
)

data class ModelMessage(
    val role: String,
    val content: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = "image/jpeg"
)

data class ModelRequest(
    val systemPrompt: String,
    val messages: List<ModelMessage>,
    val endpointConfig: EndpointConfig,
    val tools: List<ModelToolDefinition>? = null,
    val requiresVision: Boolean = false,
    val requiresTools: Boolean = false,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val responseFormatJson: Boolean = false
)

data class ModelResponse(
    val text: String,
    val modelUsed: String = "",
    val providerUsed: String = "",
    val latencyMs: Long = 0L,
    val finishReason: String? = null,
    val toolCalls: List<ModelToolCall>? = null,
    val isFallback: Boolean = false
)

data class ModelStream(
    val chunk: String,
    val providerUsed: String = "",
    val isComplete: Boolean = false
)

enum class ProviderStatus {
    ONLINE,
    DEGRADED,
    RATE_LIMITED,
    QUOTA_EXHAUSTED,
    AUTH_FAILED,
    OFFLINE,
    UNCONFIGURED
}

data class ProviderHealthStatus(
    val status: ProviderStatus,
    val latencyMs: Long = 0L,
    val lastChecked: Long = System.currentTimeMillis(),
    val message: String? = null,
    val consecutiveFailures: Int = 0,
    val cooldownUntilTimestamp: Long = 0L
)

enum class SpeedTier { FAST, BALANCED, REASONING_HEAVY }
enum class CostTier { FREE_OR_LOCAL, LOW, MEDIUM, HIGH }

data class ModelCapabilities(
    val supportsStreaming: Boolean,
    val supportsImages: Boolean,
    val supportsTools: Boolean,
    val supportsJsonSchema: Boolean = false,
    val contextWindowLength: Int = 8192,
    val maxOutputTokens: Int = 4096,
    val speedTier: SpeedTier = SpeedTier.BALANCED,
    val costTier: CostTier = CostTier.LOW
)

interface EndpointConfig {
    val url: String
    val apiKey: String
    val modelName: String
    val providerType: String get() = "OPENROUTER"
}

interface ModelProvider : CapabilityProvider {
    val providerId: String get() = name
    val modelCapabilities: ModelCapabilities
    suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus = 
        ProviderHealthStatus(status = ProviderStatus.ONLINE, latencyMs = 50L)
    suspend fun generate(request: ModelRequest): ModelResponse
    suspend fun stream(request: ModelRequest): Flow<ModelStream>
    suspend fun detectCapabilities(config: EndpointConfig): ModelCapabilities = modelCapabilities
}

// Local inference abstractions
interface LocalInferenceProvider : ModelProvider {
    suspend fun loadModel(modelPath: String)
    suspend fun unloadModel()
}

// External agent abstraction
interface ExternalAgentRuntime : CapabilityProvider {
    suspend fun executeTask(task: String, repoInfo: String): String // Returns task ID or result
    suspend fun getStatus(taskId: String): String
}

// Remote Sandbox abstraction
interface RemoteSandbox {
    suspend fun startContainer(image: String): String // sandboxId
    suspend fun executeCommand(sandboxId: String, command: String): String
    suspend fun killContainer(sandboxId: String): Boolean
}

data class CostProfile(val expectedCostTier: String)

// Coding agent abstraction
interface CodingAgentRuntime : CapabilityProvider {
    val supportedLanguages: List<String>
    val costProfile: CostProfile
    
    suspend fun inspect(repository: String): String
    suspend fun plan(task: String, context: String): String
    suspend fun modify(plan: String): Boolean
    suspend fun build(): String
    suspend fun test(): String
    suspend fun review(diff: String): Boolean
    suspend fun cancel(): Boolean
}

