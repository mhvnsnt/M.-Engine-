package com.example.ai.cloud

import kotlinx.coroutines.delay
import java.util.UUID

class AutonomousMetabolismScheduler(
    private val cycleExecutor: CycleExecutor
) {
    suspend fun runDaemon() {
        val runId = UUID.randomUUID().toString()
        println("STARTING CLOUD METABOLISM SCHEDULER (Run: $runId)")
        
        while (true) {
            val cycleId = UUID.randomUUID().toString()
            cycleExecutor.executeCycle(runId, cycleId)
            
            // Sleep before next cycle (simulating the scheduler interval)
            delay(5000L)
        }
    }
}
