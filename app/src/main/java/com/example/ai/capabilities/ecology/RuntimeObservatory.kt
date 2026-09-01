package com.example.ai.capabilities.ecology

object RuntimeObservatory {
    var lastWakeRecord: MetabolismWakeRecord? = null
    var lastSuccessfulCycle: MetabolismWakeRecord? = null
    var lastFailure: MetabolismWakeRecord? = null
    var currentActivity: String = "IDLE"
    var nextExpectedWake: Long? = null
    var queueSize: Int = 0
    var capabilityGaps: List<String> = emptyList()
    var blockedTasks: Int = 0
    
    fun recordWakeStart(record: MetabolismWakeRecord) {
        lastWakeRecord = record
        currentActivity = "WAKING"
    }
    
    fun recordWakeEnd(record: MetabolismWakeRecord) {
        if (record.result == WakeResult.SUCCESS || record.result == WakeResult.OFFLINE_PROCESSED) {
            lastSuccessfulCycle = record
        } else if (record.result == WakeResult.FAILED || record.result == WakeResult.RETRY) {
            lastFailure = record
        }
        currentActivity = "IDLE"
    }
    
    fun printStatus() {
        println("━━━━━━━━ RUNTIME OBSERVATORY ━━━━━━━━")
        println("Autonomy State: ${AutonomyControlPlane.currentState}")
        println("Current Activity: $currentActivity")
        println("Last Wake Result: ${lastWakeRecord?.result ?: "UNKNOWN"}")
        println("Last Successful Cycle: ${lastSuccessfulCycle?.actualStartTimestamp ?: "NEVER"}")
        println("Queue Size: $queueSize")
        println("Blocked Tasks: $blockedTasks")
        println("Capability Gaps: ${capabilityGaps.joinToString(", ")}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
