package com.example.ai.capabilities.ecology

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class EcologyMetabolismWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val networkAvailable = isNetworkAvailable() 
        val expectedIntervalMs = 15 * 60 * 1000L // 15 mins
        val scheduledTimestamp = RuntimeObservatory.nextExpectedWake
        val actualStart = System.currentTimeMillis()
        
        var jitterMs: Long? = null
        var scheduleStatus = WakeScheduleStatus.UNKNOWN
        
        if (scheduledTimestamp != null) {
            jitterMs = actualStart - scheduledTimestamp
            scheduleStatus = when {
                jitterMs < 60_000 -> WakeScheduleStatus.ON_SCHEDULE
                jitterMs < 300_000 -> WakeScheduleStatus.DELAYED
                else -> WakeScheduleStatus.SIGNIFICANTLY_DELAYED
            }
        }
        
        val wakeRecord = MetabolismWakeRecord(
            scheduledTimestamp = scheduledTimestamp,
            actualStartTimestamp = actualStart,
            networkAvailable = networkAvailable,
            schedulingJitterMs = jitterMs,
            scheduleStatus = scheduleStatus
        )
        
        RuntimeObservatory.recordWakeStart(wakeRecord)
        RuntimeObservatory.nextExpectedWake = actualStart + expectedIntervalMs
        
        println("━━━━━━━━ M. ENGINE — AUTONOMOUS METABOLISM WAKE ━━━━━━━━")
        println("OBSERVED: Background cycle initiated.")
        println("Run ID: ${wakeRecord.runId}")
        println("Schedule Status: $scheduleStatus (Jitter: ${jitterMs ?: "N/A"} ms)")
        
        if (AutonomyControlPlane.currentState == AutonomyState.EMERGENCY_STOP) {
            println("BLOCKED: EMERGENCY_STOP is active. Halting execution.")
            wakeRecord.complete(WakeResult.CANCELLED, exitReason = "EMERGENCY_STOP")
            RuntimeObservatory.recordWakeEnd(wakeRecord)
            return@withContext Result.success()
        } else if (AutonomyControlPlane.currentState == AutonomyState.AUTONOMY_PAUSED) {
            println("BLOCKED: Autonomy is PAUSED. Yielding.")
            wakeRecord.complete(WakeResult.BLOCKED, exitReason = "AUTONOMY_PAUSED")
            RuntimeObservatory.recordWakeEnd(wakeRecord)
            return@withContext Result.success() 
        }

        if (!networkAvailable) {
            println("OBSERVED: Device is OFFLINE. Processing local offline queue...")
            wakeRecord.complete(
                WakeResult.OFFLINE_PROCESSED,
                exitReason = "DEVICE_OFFLINE",
                nextWork = "Process remote sync when online"
            )
            RuntimeObservatory.recordWakeEnd(wakeRecord)
            return@withContext Result.success()
        }

        // Single Governor Invariant Check
        val actionId = "ecology_check_${actualStart / (15 * 60 * 1000L)}" 
        if (!IdempotencyLedger.claimExecution(actionId)) {
            println("OBSERVED: Execution for this window already processed or in progress.")
            wakeRecord.complete(WakeResult.SUCCESS, exitReason = "DUPLICATE_EXECUTION_PREVENTED")
            RuntimeObservatory.recordWakeEnd(wakeRecord)
            return@withContext Result.success()
        }

        // Delegate to LocalCapabilityAdapter (Federated Execution)
        // If the Remote Governor is active, Android yields. If unavailable, it acts as fallback.
        val executedLocally = LocalCapabilityAdapter.evaluateAndExecuteLocal(wakeRecord)

        if (!executedLocally) {
            wakeRecord.complete(WakeResult.SUCCESS, nextWork = "Yielded to Remote Governor")
        } else {
            wakeRecord.complete(WakeResult.SUCCESS, nextWork = "Local Fallback Executed")
        }

        IdempotencyLedger.markCompleted(actionId, true)
        RuntimeObservatory.recordWakeEnd(wakeRecord)
        
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Result.success()
    }
    
    var injectOfflineModeForTesting = false
    private fun isNetworkAvailable(): Boolean = !injectOfflineModeForTesting
}

object AutonomousRuntime {
    fun initialize(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<EcologyMetabolismWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ecology_metabolism",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        
        RuntimeObservatory.nextExpectedWake = System.currentTimeMillis() + (15 * 60 * 1000L)
        println("M. Engine Autonomous Runtime initialized. Work request 'ecology_metabolism' enqueued.")
    }
}
