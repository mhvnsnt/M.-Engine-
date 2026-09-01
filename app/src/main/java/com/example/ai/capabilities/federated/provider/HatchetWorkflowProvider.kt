package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class HatchetWorkflowProvider(
    private val client: HatchetClient = HatchetClient()
) : CapabilityProvider {

    override val providerId: String = "hatchet_primary"
    override val capabilityType: CapabilityType = CapabilityType.DURABLE_WORKFLOW

    override suspend fun probe(): CapabilityProbeResult {
        // Physical verification test: check if Hatchet backend is reachable
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_hatchet_engine")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: Hatchet instance unreachable at local endpoint. Physical backend not running."
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
                stderr = "CAPABILITY_GAP: Hatchet instance unreachable.",
                error = "BLOCKED: Hatchet runtime is not physically available in this environment."
            )
        }
        
        return try {
            val response = client.dispatchWorkflow("m-engine-task", task.contextPayload)
            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = 0,
                stdout = "Hatchet workflow dispatched. Response: $response",
                stderr = ""
            )
        } catch (e: Exception) {
            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown Error",
                error = "EXECUTION_FAILED"
            )
        }
    }
}
