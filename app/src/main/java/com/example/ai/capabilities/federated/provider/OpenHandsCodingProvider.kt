package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class OpenHandsCodingProvider(
    private val client: OpenHandsClient = OpenHandsClient()
) : CapabilityProvider {

    override val providerId: String = "openhands_primary"
    override val capabilityType: CapabilityType = CapabilityType.CODING

    override suspend fun probe(): CapabilityProbeResult {
        // Physical verification test: check if OpenHands backend is reachable
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_openhands_engine")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: OpenHands instance unreachable at local endpoint. Physical backend not running."
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
                stderr = "CAPABILITY_GAP: OpenHands instance unreachable.",
                error = "BLOCKED: OpenHands runtime is not physically available in this environment."
            )
        }
        
        return try {
            // Context mapping: convert M. Engine task boundaries into OpenHands Agent Canvas Session
            val payload = """
                {
                    "objective": "${task.objective}",
                    "max_iterations": ${authorization.maxBudgetTokens ?: 50},
                    "allowed_paths": [${authorization.allowedPaths.joinToString(",") { "\"$it\"" }}]
                }
            """.trimIndent()
            
            val response = client.dispatchSession(payload)
            
            // We would later return the Agent Canvas URL in the Evidence Payload for Live Observatory integration
            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = 0,
                stdout = "OpenHands session dispatched. Response: $response",
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
