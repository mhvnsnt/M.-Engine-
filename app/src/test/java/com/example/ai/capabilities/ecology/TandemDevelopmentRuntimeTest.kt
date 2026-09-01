package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TandemDevelopmentRuntimeTest {

    @Before
    fun setup() {
        SharedDevelopmentMemory.clear()
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_ENABLED
    }

    @Test
    fun testSignalIngestionAndProcessing() = runBlocking {
        val signal = DevelopmentSignal(
            type = DevelopmentSignalType.NEW_REQUIREMENT,
            project = "bannon-mechanics",
            intent = "Implement dual-queue input buffer for grappling transitions"
        )
        
        SharedDevelopmentMemory.ingestSignal(signal)
        
        assertEquals(1, SharedDevelopmentMemory.signals.value.size)
        assertEquals(SignalStatus.RECEIVED, SharedDevelopmentMemory.signals.value.first().status)

        val runtime = TandemDevelopmentRuntime()
        val budget = ExecutionBudget(maxIterations = 1, maxParallelWorkers = 3)
        val loopResult = runtime.processPendingSignalsAndEcology(budget)

        assertTrue(loopResult.iterationsCompleted > 0)
        assertTrue(loopResult.capabilityResults.isNotEmpty())
        
        // Assert signal transitioned through lifecycle
        val processedSignal = SharedDevelopmentMemory.signals.value.first()
        assertEquals(SignalStatus.HYPOTHESIZING, processedSignal.status)

        // Assert evidence was recorded in shared memory
        assertTrue(SharedDevelopmentMemory.evidenceHistory.value.isNotEmpty())
        
        // Assert runtime state reached EXPERIMENTING
        val runtimeState = SharedDevelopmentMemory.runtimeState.value
        assertEquals("EXPERIMENTING", runtimeState.currentPhase)
        assertTrue(runtimeState.currentObjective.contains("bannon-mechanics"))
    }
}
