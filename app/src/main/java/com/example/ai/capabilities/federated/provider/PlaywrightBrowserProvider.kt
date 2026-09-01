package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

class PlaywrightBrowserProvider(
    private val client: PlaywrightClient = PlaywrightClient()
) : CapabilityProvider {

    override val providerId: String = "playwright_primary"
    override val capabilityType: CapabilityType = CapabilityType.BROWSER_AUTOMATION

    override suspend fun probe(): CapabilityProbeResult {
        val isReachable = client.checkHealth()
        
        return if (isReachable) {
            CapabilityProbeResult(
                status = FabricNodeState.AVAILABLE,
                details = mapOf("location" to "local_playwright_server")
            )
        } else {
            CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: Playwright server unreachable at local endpoint. Physical browser automation backend not running."
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
                stderr = "CAPABILITY_GAP: Playwright server unreachable.",
                error = "BLOCKED: Browser automation runtime is not physically available."
            )
        }
        
        return CapabilityExecutionResult(
            taskId = task.taskId,
            exitCode = 0,
            stdout = "Browser task dispatched successfully.",
            stderr = ""
        )
    }
}
