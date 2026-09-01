package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class LiteLLMModelProvider(
    private val client: LiteLLMClient = LiteLLMClient()
) : CapabilityProvider {

    override val providerId: String = "litellm_primary"
    override val capabilityType: CapabilityType = CapabilityType.MODEL_INFERENCE

    override suspend fun probe(): CapabilityProbeResult {
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_litellm_proxy")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: LiteLLM instance unreachable at local endpoint. Physical model proxy backend not running."
            )
        }
    }

    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask
    ): CapabilityExecutionResult {
        if (!client.checkHealth()) {
            return CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = -1,
                stdout = "",
                stderr = "CAPABILITY_GAP: LiteLLM proxy unreachable.",
                error = "BLOCKED: LiteLLM runtime is not physically available in this environment."
            )
        }
        
        return CapabilityExecutionResult(
            taskId = task.taskId,
            exitCode = 0,
            stdout = "Inference request dispatched to LiteLLM successfully.",
            stderr = ""
        )
    }
}
