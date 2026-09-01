package com.example.ai.capabilities.federated.environment

enum class CapabilityLevel {
    VERIFIED,
    PARTIAL,
    UNAVAILABLE,
    UNKNOWN
}

data class EnvironmentCapabilities(
    val shellExecution: CapabilityLevel,
    val filesystemRead: CapabilityLevel,
    val filesystemWrite: CapabilityLevel,
    val processSpawning: CapabilityLevel,
    val persistentProcessSupport: CapabilityLevel,
    val networkEgress: CapabilityLevel,
    val inboundNetworkSupport: CapabilityLevel,
    val dockerCli: CapabilityLevel,
    val dockerDaemon: CapabilityLevel,
    val podman: CapabilityLevel,
    val browserAutomation: CapabilityLevel,
    val gpuAvailability: CapabilityLevel,
    val localModelRuntime: CapabilityLevel,
    val databaseAccess: CapabilityLevel,
    val secretAccess: CapabilityLevel,
    val maximumExecutionDurationMs: Long?, // null for unlimited
    val persistenceAcrossProcessDeath: CapabilityLevel
)

interface ExecutionEnvironment {
    val environmentId: String
    val environmentName: String
    val capabilities: EnvironmentCapabilities
    
    suspend fun probeCapabilities(): EnvironmentCapabilities
    suspend fun checkHealth(): Boolean = true
}
