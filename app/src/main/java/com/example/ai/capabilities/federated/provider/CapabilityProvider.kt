package com.example.ai.capabilities.federated.provider

/**
 * Universal Provider Fabric Interface
 * 
 * M. Engine connects to all mature open-source infrastructure (OpenHands, Hatchet, Playwright, etc.)
 * through this boundary. 
 * M. Engine retains the Governor / Evidence / Reality Contract authority.
 */
interface CapabilityProvider {
    val providerId: String
    val capabilityType: CapabilityType

    /**
     * Actively pings the physical infrastructure to determine its real state (e.g.,AVAILABLE, UNAVAILABLE).
     */
    suspend fun probe(): CapabilityProbeResult

    /**
     * Dispatches a bounded task to the federated provider.
     */
    suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask
    ): CapabilityExecutionResult
}
