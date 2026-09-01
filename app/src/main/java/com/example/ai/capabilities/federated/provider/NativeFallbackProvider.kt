package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

/**
 * Wraps our existing M. Engine native Android sandbox execution loops
 * behind the Universal Provider Fabric, so that we can migrate transparently
 * to OpenHands/Hatchet without breaking the system.
 */
class NativeFallbackProvider () : CapabilityProvider {

    override val providerId: String = "m_engine_native_sandbox"
    override val capabilityType: CapabilityType = CapabilityType.SANDBOX_EXECUTION

    override suspend fun probe(): CapabilityProbeResult {
        // Our native sandbox is always available as long as M. Engine is running
        return CapabilityProbeResult(
            status = FabricNodeState.AVAILABLE,
            details = mapOf("location" to "local_android_process")
        )
    }

    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask
    ): CapabilityExecutionResult {
        // Here we would route to the existing CodeMutationEngine / Sandbox manager
        return CapabilityExecutionResult(
            taskId = task.taskId,
            exitCode = 0,
            stdout = "Native Sandbox simulated execution successful",
            stderr = "",
            returnedEvidencePayload = "{\"simulated\": true}"
        )
    }
}
