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
            // The objective and its authorization boundary become the agent's
            // opening instruction. OpenHands owns execution; M. Engine keeps the
            // authority over what was permitted and what counts as evidence.
            val instruction = buildString {
                append(task.objective)
                if (authorization.allowedPaths.isNotEmpty()) {
                    append("\n\nRestrict all changes to these paths: ")
                    append(authorization.allowedPaths.joinToString(", "))
                }
            }

            val startTaskId = client.startConversation(
                instruction = instruction,
                repository = task.contextPayload.takeIf { it.isNotBlank() }
            )

            // A start task is not a finished job. Report the id and the real
            // status rather than claiming success the moment dispatch returns.
            val status = runCatching { client.startTaskStatus(startTaskId) }.getOrNull()

            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = 0,
                stdout = "OpenHands conversation started. start_task_id=$startTaskId",
                stderr = "",
                returnedEvidencePayload = status
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
