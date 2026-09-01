package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockRepositoryCapability(
    override val name: String = "MockRepositoryCapability"
) : AgencyCapability {
    override var state: CapabilityState = CapabilityState.AVAILABLE
    override var isAuthorized: Boolean = true
    override var isEnabled: Boolean = true
    override var lastHealthCheck: Long? = System.currentTimeMillis()
    override var lastSuccessfulExecution: Long? = null
    override var lastFailure: String? = null
    override var verificationEvidence: List<String> = listOf("Mock verified")
    override var currentWorkerCount: Int = 0
    override var circuitState: CircuitState = CircuitState.CLOSED
    override var nextEligibleProbe: Long? = null
    override var consecutiveSuccesses: Int = 0
    override var consecutiveFailures: Int = 0
    override val recentProbeRecords: MutableList<CapabilityProbeRecord> = mutableListOf()
    override var confidenceMetrics: EpistemicConfidenceMetrics = EpistemicConfidenceMetrics()
    override val thresholdsPolicy: TelemetryThresholdsPolicy = TelemetryThresholdsPolicy()
    
    override val transitionHistory: MutableList<CapabilityTransitionRecord> = mutableListOf()
    override val scoreConfig: CapabilityRealityScore = CapabilityRealityScore()
    override val probeType: String = "MOCK_PROBE"

    override suspend fun performHealthCheck(): HealthCheckResult = HealthCheckResult(
        capabilityId = capabilityId,
        success = true,
        latencyMs = 10L,
        evidence = verificationEvidence,
        verifiedState = CapabilityState.AVAILABLE
    )

    override suspend fun execute(context: Map<String, Any>): CapabilityResult {
        return CapabilityResult(
            observations = listOf("Mock observation"),
            evidence = listOf("Mock evidence"),
            artifactsGenerated = listOf("mock_artifact.json"),
            limitations = emptyList(),
            costMetrics = CostMetrics(networkCalls = 1, costUsd = 0.001),
            executionTimeMs = 50L,
            authorizationUsed = "MOCK_AUTH",
            failures = emptyList(),
            nextPossibilities = listOf("Next Mock Step")
        )
    }
}

class AutonomousExecutionLoopTest {

    @Before
    fun setup() {
        AutonomyControlPlane.currentState = AutonomyState.AUTONOMY_ENABLED
    }

    @Test
    fun testLoopExitsWhenBudgetExhausted() = runBlocking {
        // Budget for only 2 iterations
        val budget = ExecutionBudget(maxIterations = 2, maxParallelWorkers = 1)
        val capabilities = listOf(MockRepositoryCapability())
        val loop = AutonomousExecutionLoop(budget, capabilities)

        val dummyWakeRecord = MetabolismWakeRecord(
            scheduledTimestamp = System.currentTimeMillis(),
            actualStartTimestamp = System.currentTimeMillis(),
            networkAvailable = true,
            schedulingJitterMs = 0L,
            scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
        )

        val result = loop.run(dummyWakeRecord)

        assertEquals(2, result.iterationsCompleted)
        assertEquals(2, result.capabilityResults.size)
        assertEquals("BUDGET_EXHAUSTED", result.reasonForExit)
    }

    @Test
    fun testLoopExitsWhenNoPriorityWork() = runBlocking {
        val budget = ExecutionBudget(maxIterations = 100, maxParallelWorkers = 1)
        val capabilities = listOf(MockRepositoryCapability())
        val loop = AutonomousExecutionLoop(budget, capabilities)

        val dummyWakeRecord = MetabolismWakeRecord(
            scheduledTimestamp = System.currentTimeMillis(),
            actualStartTimestamp = System.currentTimeMillis(),
            networkAvailable = true,
            schedulingJitterMs = 0L,
            scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
        )

        val result = loop.run(dummyWakeRecord)

        assertEquals(3, result.iterationsCompleted)
        assertEquals("NO_OPPORTUNITIES_FOUND", result.reasonForExit)
    }

    @Test
    fun testParallelWorkerExecutionAcrossMultipleCapabilities() = runBlocking {
        val budget = ExecutionBudget(maxIterations = 1, maxParallelWorkers = 3)
        val cap1 = GitHubWorkerCapability().apply { state = CapabilityState.AVAILABLE }
        val cap2 = WebResearchCapability().apply { state = CapabilityState.AVAILABLE }
        val cap3 = DocumentationCapability().apply { state = CapabilityState.AVAILABLE }
        val capabilities = listOf(cap1, cap2, cap3)
        val loop = AutonomousExecutionLoop(budget, capabilities)

        val dummyWakeRecord = MetabolismWakeRecord(
            scheduledTimestamp = System.currentTimeMillis(),
            actualStartTimestamp = System.currentTimeMillis(),
            networkAvailable = true,
            schedulingJitterMs = 0L,
            scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
        )

        val result = loop.run(dummyWakeRecord)

        assertEquals(1, result.iterationsCompleted)
        assertEquals(3, result.capabilityResults.size)
        assertTrue(result.queuedOpportunities.isNotEmpty())
    }

    @Test
    fun testWorkerCancellationGovernance() = runBlocking {
        val budget = ExecutionBudget(maxIterations = 5, maxParallelWorkers = 2)
        val cap = MockRepositoryCapability()
        val loop = AutonomousExecutionLoop(budget, listOf(cap))

        // Trigger manual cycle cancellation
        loop.cancelCycle("OWNER_TEST_INTERRUPT")
        assertEquals("CANCELLED", loop.currentCycleState.status)

        val dummyWakeRecord = MetabolismWakeRecord(
            scheduledTimestamp = System.currentTimeMillis(),
            actualStartTimestamp = System.currentTimeMillis(),
            networkAvailable = true,
            schedulingJitterMs = 0L,
            scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
        )

        val result = loop.run(dummyWakeRecord)
        assertEquals("CANCELLED", result.reasonForExit)
    }
}
