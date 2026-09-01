package com.example.ai.capabilities.ecology

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class AutonomousMetabolismHardeningTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_ENABLED
        RuntimeObservatory.lastWakeRecord = null
        RuntimeObservatory.lastSuccessfulCycle = null
        RuntimeObservatory.lastFailure = null
        IdempotencyLedger.clear()
    }

    @Test
    fun testKillSwitchPreventsExecution() = runBlocking {
        AutonomyControlPlane.currentState = AutonomyState.EMERGENCY_STOP
        val worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(WakeResult.CANCELLED, RuntimeObservatory.lastWakeRecord?.result)
        assertEquals("EMERGENCY_STOP", RuntimeObservatory.lastWakeRecord?.reasonForEarlyExit)
    }
    
    @Test
    fun testPauseYieldsExecution() = runBlocking {
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_PAUSED
        val worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(WakeResult.BLOCKED, RuntimeObservatory.lastWakeRecord?.result)
        assertEquals("AUTONOMY_PAUSED", RuntimeObservatory.lastWakeRecord?.reasonForEarlyExit)
    }

    @Test
    fun testJitterCalculation() = runBlocking {
        RuntimeObservatory.nextExpectedWake = System.currentTimeMillis() - 120_000 // 2 minutes late
        
        val worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker.doWork()
        
        val record = RuntimeObservatory.lastWakeRecord
        assertNotNull(record)
        assertEquals(WakeScheduleStatus.DELAYED, record?.scheduleStatus)
        assertTrue(record!!.schedulingJitterMs!! >= 120_000)
    }
    
    @Test
    fun testIdempotencyPreventsDuplicates() = runBlocking {
        val worker1 = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker1.doWork()
        assertEquals(WakeResult.SUCCESS, RuntimeObservatory.lastWakeRecord?.result)
        assertNull(RuntimeObservatory.lastWakeRecord?.reasonForEarlyExit) // Normal processing
        
        val worker2 = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker2.doWork() // Second run in same time window hits ledger
        
        assertEquals(WakeResult.SUCCESS, RuntimeObservatory.lastWakeRecord?.result)
        assertEquals("DUPLICATE_EXECUTION_PREVENTED", RuntimeObservatory.lastWakeRecord?.reasonForEarlyExit)
    }
    
    @Test
    fun testMultiCycleEvidenceRecovery() = runBlocking {
        var worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker.doWork()
        assertNotNull(RuntimeObservatory.lastSuccessfulCycle)
        val runId1 = RuntimeObservatory.lastSuccessfulCycle!!.runId
        
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_PAUSED
        worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker.doWork()
        
        assertEquals(WakeResult.BLOCKED, RuntimeObservatory.lastWakeRecord?.result)
        assertEquals(runId1, RuntimeObservatory.lastSuccessfulCycle?.runId) // Successful state persists across aborted cycles
        
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_ENABLED
        worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker.doWork()
        
        assertTrue(RuntimeObservatory.lastWakeRecord?.result == WakeResult.SUCCESS)
    }
    
    @Test
    fun testOfflineBehavior() = runBlocking {
        val worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        worker.injectOfflineModeForTesting = true
        worker.doWork()
        
        assertEquals(WakeResult.OFFLINE_PROCESSED, RuntimeObservatory.lastWakeRecord?.result)
        assertEquals("Process remote sync when online", RuntimeObservatory.lastWakeRecord?.nextIntendedWorkCategory)
    }
}
