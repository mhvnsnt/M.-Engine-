package com.example.ai.capabilities.federated.environment

class ExecutionPlacementEngine(
    private val staticEnvironments: List<ExecutionEnvironment> = emptyList(),
    private val workerRegistry: WorkerRegistry = GlobalWorkerRegistry.instance
) {

    data class PlacementRequirement(
        val requiresDocker: Boolean = false,
        val requiresPersistentProcess: Boolean = false,
        val requiresInboundNetwork: Boolean = false,
        val requiresGpu: Boolean = false,
        val requiresBrowserAutomation: Boolean = false, val requiresUnrestrictedShell: Boolean = false
    )

    data class PlacementResult(
        val selectedEnvironment: ExecutionEnvironment?,
        val fallbackReason: String?,
        val status: String
    )

    suspend fun selectEnvironment(
        currentEnvironment: ExecutionEnvironment,
        requirement: PlacementRequirement
    ): PlacementResult {
        // 1. Check current environment first
        val currentCaps = currentEnvironment.capabilities
        if (satisfies(currentCaps, requirement)) {
            return PlacementResult(currentEnvironment, null, "CURRENT_ENVIRONMENT_SELECTED")
        }

        // 2. CURRENT ENVIRONMENT CAPABILITY GAP
        println("CURRENT ENVIRONMENT CAPABILITY GAP -> SEARCHING VERIFIED EXECUTION FABRIC")
        
        // 3. Search verified execution fabric
        val registryWorkers = workerRegistry.getVerifiedWorkers().map { RemoteFabricWorkerEnvironment("http://${it.url}:${it.hashCode()}", it.secret, initialCapabilities = it.capabilities, initialEnvironmentName = it.environmentName) }
        val alternative = (staticEnvironments + registryWorkers).find { env ->
            env.environmentId != currentEnvironment.environmentId && satisfies(env.capabilities, requirement)
        }

        if (alternative != null) {
            return PlacementResult(alternative, "Current environment lacked required capabilities", "AUTHORIZED_ALTERNATIVE_SELECTED")
        }

        // 4. Record CAPABILITY GAP
        return PlacementResult(null, "No authorized environment in the fabric satisfied the requirements", "CAPABILITY_GAP")
    }

    private fun satisfies(caps: EnvironmentCapabilities, req: PlacementRequirement): Boolean {
        if (req.requiresDocker && caps.dockerDaemon != CapabilityLevel.VERIFIED) return false
        if (req.requiresPersistentProcess && caps.persistentProcessSupport != CapabilityLevel.VERIFIED) return false
        if (req.requiresInboundNetwork && caps.inboundNetworkSupport != CapabilityLevel.VERIFIED) return false
        if (req.requiresGpu && caps.gpuAvailability != CapabilityLevel.VERIFIED) return false
        if (req.requiresBrowserAutomation && caps.browserAutomation != CapabilityLevel.VERIFIED) return false
        if (req.requiresUnrestrictedShell && caps.shellExecution != CapabilityLevel.VERIFIED) return false
        return true
    }
}
