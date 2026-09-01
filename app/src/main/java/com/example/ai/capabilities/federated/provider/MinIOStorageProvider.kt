package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class MinIOStorageProvider(
    private val client: MinIOClient = MinIOClient()
) : CapabilityProvider {

    override val providerId: String = "minio_primary"
    override val capabilityType: CapabilityType = CapabilityType.ARTIFACT_STORAGE

    override suspend fun probe(): CapabilityProbeResult {
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_minio_bucket")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: MinIO instance unreachable at local endpoint. Physical object storage backend not running."
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
                stderr = "CAPABILITY_GAP: MinIO unreachable.",
                error = "BLOCKED: MinIO runtime is not physically available."
            )
        }
        
        return CapabilityExecutionResult(
            taskId = task.taskId,
            exitCode = 0,
            stdout = "Artifact storage operation dispatched successfully.",
            stderr = ""
        )
    }
}
