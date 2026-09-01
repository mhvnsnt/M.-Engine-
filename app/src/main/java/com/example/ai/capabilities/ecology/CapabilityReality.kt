package com.example.ai.capabilities.ecology

import java.util.UUID

enum class CapabilityState {
    DECLARED,
    REGISTERED,
    IMPLEMENTED_UNVERIFIED,
    CONFIGURED,
    AUTHORIZED,
    PROBING,
    PHYSICALLY_AVAILABLE,
    PARTIALLY_VERIFIED,
    EXECUTING,
    VERIFIED_OPERATIONAL,
    AVAILABLE, // Backwards-compatible alias for verified operational
    DEGRADED,
    RECOVERING,
    CAPABILITY_GAP,
    UNAVAILABLE,
    FAILED
}

data class CapabilityTransitionRecord(
    val id: String = UUID.randomUUID().toString().take(8),
    val capabilityId: String,
    val fromState: CapabilityState,
    val toState: CapabilityState,
    val probeType: String,
    val latencyMs: Long,
    val evidence: List<String>,
    val failureReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FailureClassification {
    TRANSIENT_NETWORK_FAILURE,
    TIMEOUT,
    RATE_LIMITED,
    AUTHORIZATION_FAILURE,
    CONFIGURATION_FAILURE,
    DEPENDENCY_UNAVAILABLE,
    WORKER_CRASH,
    INVALID_RESPONSE,
    PROBE_IMPLEMENTATION_FAILURE,
    POLICY_BLOCKED,
    UNKNOWN_FAILURE
}

enum class CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

data class CapabilityProbeRecord(
    val probeId: String = UUID.randomUUID().toString(),
    val capabilityId: String,
    val workerId: String? = null,
    val probeStartTimestamp: Long,
    val probeEndTimestamp: Long,
    val latencyMs: Long,
    val result: String, // "SUCCESS", "FAILURE", "TIMEOUT"
    val failureClassification: FailureClassification? = null,
    val retryCount: Int = 0,
    val consecutiveSuccesses: Int = 0,
    val consecutiveFailures: Int = 0,
    val lastSuccessfulProbe: Long? = null,
    val lastFailedProbe: Long? = null,
    val evidenceReference: String? = null,
    val executionCost: Double = 0.0,
    val networkCost: Double = 0.0,
    val autonomyLevel: String = "HIGH"
)

data class CapabilityRealityScore(
    val ownerRelevance: Double = 5.0,        // 1.0 .. 10.0
    val architecturalDependency: Double = 5.0,// 1.0 .. 10.0
    val capabilityUncertainty: Double = 5.0,  // 1.0 .. 10.0
    val easeOfVerification: Double = 5.0,     // 1.0 .. 10.0
    val recoveryValue: Double = 5.0,          // 1.0 .. 10.0
    val verificationCost: Double = 1.0,       // 1.0 .. 10.0
    val verificationRisk: Double = 1.0,       // 1.0 .. 10.0
    val retryBurden: Double = 1.0             // 1.0 .. 10.0
) {
    /**
     * Opportunity Engine Ranking Formula:
     * (Relevance × Dependency × Uncertainty × Ease × RecoveryValue) / max(1.0, Cost + Risk + RetryBurden)
     */
    val score: Double
        get() {
            val numerator = ownerRelevance * architecturalDependency * capabilityUncertainty * easeOfVerification * recoveryValue
            val denominator = maxOf(1.0, verificationCost + verificationRisk + retryBurden)
            return numerator / denominator
        }
}

data class CapabilityRealityRankingItem(
    val capabilityId: String,
    val capabilityType: String,
    val currentState: CapabilityState,
    val score: CapabilityRealityScore,
    val rank: Int = 1,
    val recommendedProbeType: String = "HEALTH_PING"
)

data class RealitySweepReport(
    val sweepId: String = "sweep-${UUID.randomUUID().toString().take(8)}",
    val timestamp: Long = System.currentTimeMillis(),
    val rankings: List<CapabilityRealityRankingItem> = emptyList(),
    val transitionsExecuted: List<CapabilityTransitionRecord> = emptyList(),
    val verifiedOperationalCount: Int = 0,
    val physicalAvailableCount: Int = 0,
    val unverifiedCount: Int = 0,
    val capabilityGapCount: Int = 0,
    val totalCapabilities: Int = 0,
    val summary: String = ""
)

data class EpistemicConfidenceMetrics(
    val implementationConfidence: Double = 0.0,
    val configurationConfidence: Double = 0.0,
    val historicalAvailabilityConfidence: Double = 0.0,
    val currentAvailabilityConfidence: Double = 0.0,
    val degradationConfidence: Double = 0.0
)

data class TelemetryThresholdsPolicy(
    val maxRetries: Int = 3,
    val circuitOpenFailureCount: Int = 3,
    val circuitHalfOpenDelayMs: Long = 10000L,
    val staleSuccessThresholdMs: Long = 60000L,
    val baseRetryDelayMs: Long = 1000L,
    val maxRetryDelayMs: Long = 30000L
)

data class HealthCheckResult(
    val capabilityId: String,
    val success: Boolean,
    val latencyMs: Long,
    val evidence: List<String>,
    val verifiedState: CapabilityState,
    val failureReason: String? = null,
    val failureClassification: FailureClassification? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val transitionRecord: CapabilityTransitionRecord? = null,
    val probeRecord: CapabilityProbeRecord? = null,
    val granularStatus: Map<String, CapabilityState> = emptyMap()
)

data class CapabilityRuntimeState(
    val capabilityId: String,
    val capabilityType: String,
    val registered: Boolean,
    val configured: Boolean,
    val authorized: Boolean,
    val available: Boolean,
    val state: CapabilityState,
    val circuitState: CircuitState = CircuitState.CLOSED,
    val nextEligibleProbe: Long? = null,
    val lastHealthCheck: Long? = null,
    val lastSuccessfulExecution: Long? = null,
    val lastFailure: String? = null,
    val currentWorkerCount: Int = 0,
    val maximumWorkerCount: Int = 3,
    val costBudget: Double = 1.0,
    val remainingBudget: Double = 1.0,
    val environmentIdentity: String = "local-sandbox",
    val verificationEvidence: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val realityScore: Double = 0.0,
    val rank: Int = 0,
    val probeType: String = "GENERIC_PROBE",
    val recentTransitions: List<CapabilityTransitionRecord> = emptyList(),
    val confidenceMetrics: EpistemicConfidenceMetrics = EpistemicConfidenceMetrics(),
    val recentProbeRecords: List<CapabilityProbeRecord> = emptyList(),
    val granularStatus: Map<String, CapabilityState> = emptyMap()
)

enum class WorkerJobState {
    QUEUED,
    DISPATCHED,
    EXECUTING,
    WAITING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BUDGET_EXHAUSTED,
    CAPABILITY_GAP
}

data class WorkerJob(
    val workerId: String = UUID.randomUUID().toString().take(8),
    val parentCycleId: String,
    val capabilityId: String,
    val objective: String,
    var state: WorkerJobState = WorkerJobState.QUEUED,
    val startedAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var costConsumed: CostMetrics = CostMetrics(),
    var evidenceProduced: List<String> = emptyList(),
    var resultClassification: String? = null,
    var failureReason: String? = null
)

data class AutonomousCycleState(
    val cycleId: String,
    val objective: String,
    val initialBudget: ExecutionBudget,
    var budgetConsumed: CostMetrics = CostMetrics(),
    var budgetRemaining: ExecutionBudget = initialBudget.copy(),
    val startTime: Long = System.currentTimeMillis(),
    val deadline: Long = startTime + initialBudget.maxExecutionTimeMs,
    val workerJobs: MutableList<WorkerJob> = mutableListOf(),
    var exitReason: String? = null,
    var status: String = "EXECUTING" // EXECUTING, COMPLETED, BUDGET_EXHAUSTED, CANCELLED, FAILED
)

data class CapabilityTelemetry(
    val activeWorkers: Int = 0,
    val queuedWorkers: Int = 0,
    val completedWorkers: Int = 0,
    val failedWorkers: Int = 0,
    val averageExecutionTime: Long = 0L,
    val budgetConsumption: Double = 0.0,
    val capabilityAvailability: Map<String, CapabilityState> = emptyMap(),
    val lastHeartbeat: Long = System.currentTimeMillis()
) {
    fun isStale(staleThresholdMs: Long = 15000L, currentTime: Long = System.currentTimeMillis()): Boolean {
        return (currentTime - lastHeartbeat) > staleThresholdMs
    }
}

data class CausalDevelopmentRecord(
    val id: String = UUID.randomUUID().toString(),
    val humanSignalId: String? = null,
    val humanSignalType: String? = null,
    val humanIntent: String? = null,
    val opportunityId: String? = null,
    val opportunityDescription: String? = null,
    val dispatchedWorkerId: String? = null,
    val capabilityId: String? = null,
    val experimentDescription: String? = null,
    val evidenceArtifact: String? = null,
    val proposedPatch: String? = null,
    val verificationOutcome: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
