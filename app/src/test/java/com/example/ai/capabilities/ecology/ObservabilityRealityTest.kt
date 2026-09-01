package com.example.ai.capabilities.ecology

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ObservabilityRealityTest {

    @Before
    fun setup() {
        FederatedCapabilityRegistry.clear()
        SharedDevelopmentMemory.clear()
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_ENABLED
    }

    @Test
    fun testAtomicBudgetGovernorPreventsOversubscriptionUnderParallelPressure() = runBlocking {
        val initialBudget = ExecutionBudget(
            maxIterations = 10,
            maxParallelWorkers = 3,
            maxNetworkCalls = 5,
            maxHighCostModelCalls = 2,
            maxCostUsd = 0.10
        )
        val governor = AtomicBudgetGovernor(initialBudget)

        // Launch 10 parallel reservation attempts where each requests 1 network call and 1 model call
        val attempts: List<BudgetReservation?> = (1..10).map { id ->
            async {
                governor.tryReserve(
                    workerId = "worker-$id",
                    actions = 1,
                    networkCalls = 1,
                    modelCalls = 1,
                    costUsd = 0.05
                )
            }
        }.awaitAll()

        val successfulCount = attempts.count { it != null }
        // Should only succeed for 2 workers because maxHighCostModelCalls = 2 and maxCostUsd = 0.10 (2 * 0.05 = 0.10)
        assertEquals(2, successfulCount)

        val remaining = governor.getRemainingBudget()
        assertEquals(8, remaining.maxIterations)
        assertEquals(0, remaining.maxHighCostModelCalls)
        assertEquals(3, remaining.maxNetworkCalls)
        assertEquals(0.0, remaining.maxCostUsd, 0.001)
    }

    @Test
    fun testCapabilityRealityLifecycle() = runBlocking {
        val cap = GitHubWorkerCapability()
        FederatedCapabilityRegistry.register(cap)

        // Phase 1: Initially implemented but unverified
        assertEquals(CapabilityState.IMPLEMENTED_UNVERIFIED, cap.state)
        assertFalse(cap.isAvailable())

        // Phase 2: Run physical health check
        val healthResult = cap.verifyHealth()
        assertTrue(healthResult.success)
        assertEquals(CapabilityState.AVAILABLE, healthResult.verifiedState)
        assertEquals(CapabilityState.AVAILABLE, cap.state)
        assertTrue(cap.isAvailable())
        assertTrue(cap.verificationEvidence.isNotEmpty())

        // Phase 3: Owner toggling capability
        FederatedCapabilityRegistry.toggleCapability(cap.capabilityId, false)
        assertFalse(cap.isEnabled)
        assertFalse(cap.isAvailable())

        FederatedCapabilityRegistry.toggleCapability(cap.capabilityId, true)
        assertTrue(cap.isEnabled)
        assertTrue(cap.isAvailable())
    }

    @Test
    fun testTandemCoDevelopmentCausalTraceability() = runBlocking {
        val signal = DevelopmentSignal(
            type = DevelopmentSignalType.NEW_REQUIREMENT,
            project = "bannon-mechanics",
            intent = "Add input buffering to grapple transitions"
        )
        SharedDevelopmentMemory.ingestSignal(signal)

        assertEquals(1, SharedDevelopmentMemory.signals.value.size)
        assertEquals(SignalStatus.RECEIVED, SharedDevelopmentMemory.signals.value.first().status)

        val tandemRuntime = TandemDevelopmentRuntime()
        val loopResult = tandemRuntime.processPendingSignalsAndEcology(
            budget = ExecutionBudget(maxIterations = 1, maxParallelWorkers = 2)
        )

        assertTrue(loopResult.capabilityResults.isNotEmpty())
        assertEquals(SignalStatus.HYPOTHESIZING, SharedDevelopmentMemory.signals.value.first().status)

        val causalRecords = SharedDevelopmentMemory.causalRecords.value
        assertTrue("Expected causal development links to be established", causalRecords.isNotEmpty())
        val firstCausal = causalRecords.first()
        assertEquals(signal.id, firstCausal.humanSignalId)
        assertEquals("Add input buffering to grapple transitions", firstCausal.humanIntent)
        assertNotNull(firstCausal.opportunityId)
        assertNotNull(firstCausal.dispatchedWorkerId)
        assertEquals("VERIFIED", firstCausal.verificationOutcome)
    }

    @Test
    fun testTelemetryStaleDetection() {
        val liveTelemetry = CapabilityTelemetry(
            activeWorkers = 2,
            lastHeartbeat = System.currentTimeMillis() - 2000L
        )
        assertFalse(liveTelemetry.isStale(15000L))

        val staleTelemetry = CapabilityTelemetry(
            activeWorkers = 0,
            lastHeartbeat = System.currentTimeMillis() - 20000L
        )
        assertTrue(staleTelemetry.isStale(15000L))
    }
}
