package com.example.ai.capabilities.acquisition

import kotlinx.coroutines.delay

class CapabilityAcquisitionManager {
    
    val physicalDiscovery = PhysicalRuntimeDiscovery()

    suspend fun discoverOpenHandsRuntime(
        envUrl: String?,
        envKey: String?
    ): CapabilityObservation {
        // Physical Probe first
        val probes = physicalDiscovery.executeProbes()
        val dockerDaemon = probes.find { it.toolName == "Docker Daemon" }
        
        // 1. Discover local OSS / self-hosted tools
        val localNetworkReachable = pingLocalhost("http://localhost:3000/api/version")
        if (localNetworkReachable) {
            return CapabilityObservation(
                capabilityName = "OpenHands",
                implementationStatus = "implemented",
                discoveryState = DiscoveryState.DISCOVERED,
                authenticationState = "NOT REQUIRED",
                authReason = "Local runtime requires no authentication.",
                nextAutonomousOptions = listOf("Use discovered local container/runtime")
            )
        }
        
        // Check if we can provision
        if (dockerDaemon?.lifecycleState == CapabilityLifecycleState.REACHABLE) {
             return CapabilityObservation(
                capabilityName = "OpenHands",
                implementationStatus = "implemented",
                discoveryState = DiscoveryState.RUNTIME_NOT_PROVISIONED,
                authenticationState = "NOT EVALUATED",
                authReason = "Runtime not discovered.",
                nextAutonomousOptions = listOf(
                    "PROVISIONING OPPORTUNITY: Compatible local execution substrate exists (Docker).",
                    "PROPOSED ACTION: Provision isolated OpenHands worker.",
                    "AUTHORIZATION: REQUIRES_PROVISIONING_APPROVAL",
                    "Use alternative native worker fallback."
                )
            )
        }

        // 2. Discover via provided managed secret if available (fallback)
        if (!envUrl.isNullOrEmpty()) {
            val remoteReachable = pingRemote(envUrl)
            if (remoteReachable) {
                if (envKey.isNullOrEmpty()) {
                    return CapabilityObservation(
                        capabilityName = "OpenHands",
                        implementationStatus = "implemented",
                        discoveryState = DiscoveryState.DISCOVERED,
                        authenticationState = "REQUIRED",
                        authReason = "Remote endpoint provisioned but requires managed secret or one-tap auth.",
                        nextAutonomousOptions = listOf("Offer one-tap cloud connector", "Provide managed secret")
                    )
                } else {
                     return CapabilityObservation(
                        capabilityName = "OpenHands",
                        implementationStatus = "implemented",
                        discoveryState = DiscoveryState.DISCOVERED,
                        authenticationState = "VERIFIED",
                        authReason = "Managed secret provided.",
                        nextAutonomousOptions = listOf("Execute bounded automation trials")
                    )
                }
            } else {
                 return CapabilityObservation(
                    capabilityName = "OpenHands",
                    implementationStatus = "implemented",
                    discoveryState = DiscoveryState.RUNTIME_UNREACHABLE,
                    authenticationState = "NOT EVALUATED",
                    authReason = "Configured remote runtime is unreachable.",
                    nextAutonomousOptions = listOf("Verify infrastructure connectivity")
                )
            }
        }

        // 3. Fallback: Capability Gap recorded. Do not repeatedly request.
        return CapabilityObservation(
            capabilityName = "OpenHands",
            implementationStatus = "implemented",
            discoveryState = DiscoveryState.RUNTIME_NOT_PROVISIONED,
            authenticationState = "NOT EVALUATED",
            authReason = "no runtime exists yet to authenticate against.",
            nextAutonomousOptions = listOf(
                "Discover local container/runtime support.",
                "Use existing native coding sandbox.",
                "Offer one-tap cloud connector if later desired."
            )
        )
    }
    
    fun selectWorkerFallback(): String {
        return "NATIVE_SANDBOX_WORKER"
    }

    private suspend fun pingLocalhost(url: String): Boolean {
        delay(100) // Physical ping abstraction
        return false // Assume no local container running for now
    }

    private suspend fun pingRemote(url: String): Boolean {
        delay(100)
        return true // Assume remote is alive if configured
    }
}

class NativeWorkerFallbackSelector {
    
    fun selectBestAvailableWorker(openHandsObservation: CapabilityObservation): String {
        if (openHandsObservation.discoveryState == DiscoveryState.DISCOVERED && openHandsObservation.authenticationState == "VERIFIED") {
            return "VERIFIED_OPENHANDS_WORKER"
        }
        
        // Mocking alternatives for now
        val hasVerifiedAlternative = false
        if (hasVerifiedAlternative) {
            return "VERIFIED_ALTERNATIVE_CODING_WORKER"
        }
        
        val hasNativeSandbox = true
        if (hasNativeSandbox) {
             return "NATIVE_SANDBOX_WORKER"
        }
        
        return "RESEARCH_INSPECTION_ONLY_MODE"
    }
}
