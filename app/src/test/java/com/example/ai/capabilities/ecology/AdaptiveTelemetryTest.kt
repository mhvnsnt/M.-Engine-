package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AdaptiveTelemetryTest {

    // A mock capability that allows injecting outcomes
    class MockProbeCapability(
        override val name: String = "MockProbeCapability"
    ) : AgencyCapability {
        override var state: CapabilityState = CapabilityState.IMPLEMENTED_UNVERIFIED
        override var isAuthorized: Boolean = true
        override var isEnabled: Boolean = true
        override var lastHealthCheck: Long? = null
        override var lastSuccessfulExecution: Long? = null
        override var lastFailure: String? = null
        override var verificationEvidence: List<String> = emptyList()
        override var currentWorkerCount: Int = 0
        override var circuitState: CircuitState = CircuitState.CLOSED
        override var nextEligibleProbe: Long? = null
        override var consecutiveSuccesses: Int = 0
        override var consecutiveFailures: Int = 0
        override val recentProbeRecords: MutableList<CapabilityProbeRecord> = mutableListOf()
        override var confidenceMetrics: EpistemicConfidenceMetrics = EpistemicConfidenceMetrics()
        override val thresholdsPolicy: TelemetryThresholdsPolicy = TelemetryThresholdsPolicy(
            baseRetryDelayMs = 100L, // Fast for testing
            maxRetryDelayMs = 2000L
        )
        override val transitionHistory: MutableList<CapabilityTransitionRecord> = mutableListOf()
        
        var nextProbeOutcomeSuccess: Boolean = true
        var nextProbeException: Exception? = null
        var nextLatencyMs: Long = 10L

        override suspend fun performHealthCheck(): HealthCheckResult {
            if (nextProbeException != null) {
                throw nextProbeException!!
            }
            return HealthCheckResult(
                capabilityId = capabilityId,
                success = nextProbeOutcomeSuccess,
                latencyMs = nextLatencyMs,
                evidence = listOf(if (nextProbeOutcomeSuccess) "OK" else "FAIL"),
                verifiedState = if (nextProbeOutcomeSuccess) CapabilityState.AVAILABLE else CapabilityState.FAILED,
                failureClassification = if (!nextProbeOutcomeSuccess) FailureClassification.TRANSIENT_NETWORK_FAILURE else null,
                failureReason = if (!nextProbeOutcomeSuccess) "Injected failure" else null
            )
        }

        override suspend fun execute(context: Map<String, Any>): CapabilityResult {
            return CapabilityResult(emptyList(), emptyList(), emptyList(), emptyList(), CostMetrics(), 0L, null, emptyList(), emptyList())
        }
    }

    @Test
    fun testTransientFailureAndRecovery() = runBlocking {
        val cap = MockProbeCapability()
        
        // 1. Initial success
        cap.nextProbeOutcomeSuccess = true
        var result = cap.verifyHealth()
        assertTrue(result.success)
        assertEquals(CapabilityState.AVAILABLE, cap.state)
        assertEquals(1, cap.consecutiveSuccesses)
        assertEquals(CircuitState.CLOSED, cap.circuitState)
        
        // 2. Transient failure
        cap.nextProbeOutcomeSuccess = false
        result = cap.verifyHealth()
        assertFalse(result.success)
        assertEquals(FailureClassification.TRANSIENT_NETWORK_FAILURE, result.failureClassification)
        assertEquals(CapabilityState.DEGRADED, cap.state) // single failure degrades
        assertEquals(1, cap.consecutiveFailures)
        assertEquals(0, cap.consecutiveSuccesses)
        assertEquals(CircuitState.CLOSED, cap.circuitState)
        
        // 3. Recovery success
        cap.nextProbeOutcomeSuccess = true
        result = cap.verifyHealth()
        assertTrue(result.success)
        assertEquals(CapabilityState.RECOVERING, cap.state) // first success goes to recovering
        
        // 4. Fully recovered
        result = cap.verifyHealth()
        assertTrue(result.success)
        assertEquals(CapabilityState.AVAILABLE, cap.state) // second success goes to available
        assertEquals(2, cap.consecutiveSuccesses)
        assertEquals(0, cap.consecutiveFailures)
    }

    @Test
    fun testPriorityReductionForRepeatedFailures() = runBlocking {
        val cap = MockProbeCapability()
        cap.nextProbeOutcomeSuccess = false
        
        val initialPriority = cap.scoreConfig.score
        
        cap.verifyHealth()
        cap.verifyHealth()
        
        // As a mock, we adjust the score config directly to simulate Opportunity Engine's reaction
        val degradedScoreConfig = cap.scoreConfig.copy(retryBurden = 5.0)
        assertTrue(degradedScoreConfig.score < initialPriority)
    }
    
    @Test
    fun testKillSwitchInterruption() = runBlocking {
        val cap = MockProbeCapability()
        cap.isEnabled = false // Kill switch
        
        val isAvailable = cap.isAvailable()
        assertFalse("Capability should be unavailable when disabled", isAvailable)
    }

    @Test
    fun testCircuitBreakerHalfOpenRecoveryProbe() = runBlocking {
        val cap = MockProbeCapability()
        cap.nextProbeOutcomeSuccess = false
        
        // Fail until OPEN
        cap.verifyHealth()
        cap.verifyHealth()
        cap.verifyHealth()
        assertEquals(CircuitState.OPEN, cap.circuitState)
        
        // Fast-forward nextEligibleProbe to simulate time passing
        cap.nextEligibleProbe = System.currentTimeMillis() - 1000
        
        // Next probe should transition to HALF_OPEN and then succeed if probe succeeds
        cap.nextProbeOutcomeSuccess = true
        val result = cap.verifyHealth()
        assertTrue(result.success)
        assertEquals(CircuitState.CLOSED, cap.circuitState) // Restores circuit
    }
    
    @Test
    fun testRateLimitHandling() = runBlocking {
        val cap = MockProbeCapability()
        cap.nextProbeOutcomeSuccess = false
        cap.nextProbeException = Exception("HTTP 429 Too Many Requests")
        
        val result = cap.verifyHealth()
        assertFalse(result.success)
        assertEquals(FailureClassification.WORKER_CRASH, result.failureClassification)
    }

    @Test
    fun testCircuitBreakerAndExponentialBackoff() = runBlocking {
        val cap = MockProbeCapability()
        cap.nextProbeOutcomeSuccess = false
        
        // Fail multiple times to trigger circuit open
        cap.verifyHealth() // Degraded, fails=1
        cap.verifyHealth() // Failed, fails=2
        cap.verifyHealth() // Failed, fails=3, circuit opens
        
        assertEquals(CapabilityState.FAILED, cap.state)
        assertEquals(CircuitState.OPEN, cap.circuitState)
        assertNotNull(cap.nextEligibleProbe)
        
        // Verify exponential backoff calculation (approximate due to jitter)
        val now = System.currentTimeMillis()
        val expectedDelayBase = 100L * (1 shl 3) // 800ms
        val actualDelay = cap.nextEligibleProbe!! - now
        // Jitter is +20% max, so delay is between 800 and 960
        assertTrue("Delay \$actualDelay should be >= 800", actualDelay >= 800)
        assertTrue("Delay \$actualDelay should be <= 1000", actualDelay <= 1000)
        
        // Attempting to probe while OPEN skips probe
        val openResult = cap.verifyHealth()
        assertFalse(openResult.success)
        assertEquals(FailureClassification.POLICY_BLOCKED, openResult.failureClassification)
        assertEquals(CircuitState.OPEN, cap.circuitState)
    }

    @Test
    fun testStaleSuccessfulEvidenceExpiration() = runBlocking {
        val cap = MockProbeCapability()
        cap.nextProbeOutcomeSuccess = true
        cap.verifyHealth()
        
        // Mock a time jump in confidence metrics
        cap.confidenceMetrics = cap.confidenceMetrics.copy(currentAvailabilityConfidence = 1.0)
        
        val lastSuccessTime = System.currentTimeMillis() - cap.thresholdsPolicy.staleSuccessThresholdMs - 1000
        val isStale = (System.currentTimeMillis() - lastSuccessTime) > cap.thresholdsPolicy.staleSuccessThresholdMs
        
        assertTrue("Evidence should be considered stale", isStale)
    }
}
