package com.example.ai.cloud

interface AgencyLedgerRepository {
    fun initDatabase()
    fun isEmergencyStopActive(): Boolean
    fun setEmergencyStop(active: Boolean)
    fun isAutonomyEnabled(): Boolean
    fun setAutonomyEnabled(enabled: Boolean)
    fun startCycle(cycleId: String, runId: String)
    fun getCycleStatus(cycleId: String): String?
    fun completeCycle(cycleId: String, exitReason: String)
    fun failCycle(cycleId: String, exitReason: String)
    fun emitMindstream(cycleId: String, entryType: String, content: String)
    fun getMindstream(): List<String>
    fun getPendingOpportunities(): List<String>
    fun addOpportunity(description: String, source: String)

    // Federated Capability Reality
    fun getCapabilities(): List<Map<String, Any>>
    fun verifyCapability(id: String): Map<String, Any>
    fun runRealitySweep(): Map<String, Any>
    fun getCapabilityTransitions(): List<Map<String, Any>>
    fun toggleCapability(id: String, enabled: Boolean): Map<String, Any>

    // Parallel Worker Streams & Active Cycle
    fun getActiveCycle(): Map<String, Any>?
    fun cancelCycle(cycleId: String): Boolean
    fun cancelWorker(workerId: String): Boolean

    // Live Telemetry
    fun getTelemetry(): Map<String, Any>

    // Tandem Co-Development
    fun getTandemDevelopment(): Map<String, Any>
    fun recordDevelopmentSignal(type: String, project: String, intent: String): Map<String, Any>
}
