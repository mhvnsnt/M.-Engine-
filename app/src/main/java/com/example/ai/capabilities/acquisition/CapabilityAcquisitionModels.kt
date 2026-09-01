package com.example.ai.capabilities.acquisition

enum class ConnectionMode {
    LOCAL_NO_AUTH,
    LOCAL_NETWORK,
    ONE_TAP_OAUTH,
    DEVICE_AUTHORIZATION,
    MANAGED_SECRET,
    CAPABILITY_UNAVAILABLE
}

enum class CapabilityLifecycleState {
    NOT_DISCOVERED,
    DISCOVERED,
    REACHABLE,
    IDENTIFIED,
    AUTHORIZED,
    MINIMAL_OPERATION_VERIFIED,
    TASK_OPERATION_VERIFIED,
    RELIABILITY_UNDER_OBSERVATION
}

enum class DiscoveryState {
    RUNTIME_NOT_PROVISIONED,
    RUNTIME_UNREACHABLE,
    AUTH_REQUIRED,
    AUTH_FAILED,
    DISCOVERED,
    PARTIALLY_AVAILABLE
}

data class PhysicalProbeObservation(
    val toolName: String,
    val lifecycleState: CapabilityLifecycleState,
    val evidence: String,
    val stdout: String? = null,
    val stderr: String? = null,
    val exitCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun printLedger() {
        println("OBSERVATION: " + toolName)
        println("State: " + lifecycleState)
        println("Evidence: " + evidence)
        if (exitCode != null) println("Exit Code: " + exitCode)
        if (!stdout.isNullOrEmpty()) println("Stdout: " + stdout.take(200).replace('\n', ' '))
        if (!stderr.isNullOrEmpty()) println("Stderr: " + stderr.take(200).replace('\n', ' '))
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}

data class CapabilityObservation(
    val capabilityName: String,
    val implementationStatus: String,
    val discoveryState: DiscoveryState,
    val authenticationState: String,
    val authReason: String,
    val nextAutonomousOptions: List<String>
) {
    fun printLedger() {
        println("━━━━━━━━ M. ENGINE — CAPABILITY ACQUISITION ━━━━━━━━")
        println("OBSERVED")
        println(capabilityName + " adapter " + implementationStatus + ".")
        println()
        println("CAPABILITY DISCOVERY")
        println(when(discoveryState) {
            DiscoveryState.RUNTIME_NOT_PROVISIONED -> "No reachable authorized runtime discovered."
            DiscoveryState.RUNTIME_UNREACHABLE -> "Runtime known but unreachable."
            DiscoveryState.DISCOVERED -> "Authorized runtime discovered."
            DiscoveryState.PARTIALLY_AVAILABLE -> "Runtime discovered with partial capabilities."
            else -> discoveryState.name
        })
        println()
        println("AUTHENTICATION")
        println(authenticationState)
        println("Reason: " + authReason)
        println()
        println("NEXT AUTONOMOUS OPTIONS")
        nextAutonomousOptions.forEachIndexed { index, option ->
            println("" + (index + 1) + ". " + option)
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
