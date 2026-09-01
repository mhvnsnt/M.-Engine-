package com.example.ai.capabilities.federated

import com.example.ai.capabilities.acquisition.CapabilityAcquisitionManager
import com.example.ai.capabilities.acquisition.CapabilityObservation

/**
 * MISSION 17.2D.5A.1 — OpenHands Capability Probe (Zero-Config / One-Tap)
 * 
 * Determines whether an OpenHands runtime is available locally, over the network,
 * or via provided credentials, without defaulting to hard failure if an API key is missing.
 */
class OpenHandsCapabilityProbe(
    private val acquisitionManager: CapabilityAcquisitionManager = CapabilityAcquisitionManager()
) {
    suspend fun executeProbe(endpointUrl: String?, apiKey: String?): CapabilityObservation {
        return acquisitionManager.discoverOpenHandsRuntime(endpointUrl, apiKey)
    }
}
