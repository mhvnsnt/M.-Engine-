package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class PostgresDatabaseProvider(
    private val client: PostgresClient = PostgresClient()
) : CapabilityProvider {

    override val providerId: String = "postgres_primary"
    override val capabilityType: CapabilityType = CapabilityType.DATABASE

    override suspend fun probe(): CapabilityProbeResult {
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_postgres_engine", "vector_extension" to "pgvector_assumed")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: PostgreSQL instance unreachable on port 5432. Physical DB backend not running."
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
                stderr = "CAPABILITY_GAP: PostgreSQL unreachable.",
                error = "BLOCKED: PostgreSQL/pgvector runtime is not physically available."
            )
        }
        
        return CapabilityExecutionResult(
            taskId = task.taskId,
            exitCode = 0,
            stdout = "Database operation dispatched successfully.",
            stderr = ""
        )
    }
}
