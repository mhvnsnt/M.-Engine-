package com.example.ai.capabilities

import kotlinx.coroutines.flow.Flow

data class ModelRequest(
    val systemPrompt: String,
    val messages: List<ModelMessage>,
    val endpointConfig: EndpointConfig
)

data class ModelMessage(
    val role: String,
    val content: String
)

data class ModelResponse(
    val text: String
)

data class ModelStream(
    val chunk: String
)

data class ModelCapabilities(
    val supportsStreaming: Boolean,
    val supportsImages: Boolean,
    val supportsTools: Boolean,
    val contextWindowLength: Int
)

interface EndpointConfig {
    val url: String
    val apiKey: String
    val modelName: String
}

interface ModelProvider : CapabilityProvider {
    val modelCapabilities: ModelCapabilities
    suspend fun generate(request: ModelRequest): ModelResponse
    suspend fun stream(request: ModelRequest): Flow<ModelStream>
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

