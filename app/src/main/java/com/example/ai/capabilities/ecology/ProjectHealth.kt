package com.example.ai.capabilities.ecology

enum class ProjectHealth {
    HEALTHY, STALE, BROKEN, UNDERDEVELOPED, HIGH_POTENTIAL, RESEARCH_REQUIRED, ARCHIVED_CANDIDATE, UNKNOWN
}

enum class HealthState { HEALTHY, FAILING, UNKNOWN, OBSERVED }
enum class DependencyState { OUTDATED, CURRENT, UNKNOWN, DEPRECATED, VULNERABLE, INTENTIONALLY_PINNED }
enum class ActivityState { STABLE_LOW_ACTIVITY, ABANDONED_CANDIDATE, ACTIVELY_DEVELOPED, UNKNOWN }
enum class IssuePressureState { LOW_PRESSURE, MODERATE_PRESSURE, HIGH_PRESSURE, UNKNOWN }
enum class RelevanceState { OBSERVED_RELEVANCE, INFERRED_RELEVANCE, HYPOTHESIZED_RELEVANCE, UNKNOWN }

data class CapabilityGap(
    val missingCapability: String,
    val locallyObtainable: Boolean,
    val acquisitionCost: String,
    val securityImplications: String,
    val reproducibilityImpact: String,
    val alternativeEnvironment: String,
    val authorizationRequired: String
)

data class HealthDimensionRecord<T>(
    val value: T,
    val confidence: Double,
    val evidenceReferences: List<String>,
    val sourceCommitSha: String?,
    val inspectionTimestamp: Long = System.currentTimeMillis(),
    val freshnessPolicy: String = "EXPIRES_ON_NEW_COMMIT",
    val uncertaintyReason: String? = null,
    val falsificationCondition: String? = null,
    val recommendedNextAction: String? = null,
    val capabilityGap: CapabilityGap? = null,
    var evidenceStatus: EvidenceStatus = EvidenceStatus.CURRENT
) {
    fun isValidFor(commitSha: String?): Boolean {
        return this.sourceCommitSha != null && this.sourceCommitSha == commitSha
    }
}

data class HealthEvidenceMatrix(
    var structuralHealth: HealthDimensionRecord<HealthState> = createUnknown(HealthState.UNKNOWN),
    var buildHealth: HealthDimensionRecord<HealthState> = createUnknown(HealthState.UNKNOWN),
    var testHealth: HealthDimensionRecord<HealthState> = createUnknown(HealthState.UNKNOWN),
    var dependencyFreshness: HealthDimensionRecord<DependencyState> = createUnknown(DependencyState.UNKNOWN),
    var activity: HealthDimensionRecord<ActivityState> = createUnknown(ActivityState.UNKNOWN),
    var issuePressure: HealthDimensionRecord<IssuePressureState> = createUnknown(IssuePressureState.UNKNOWN),
    var architecturalComplexity: HealthDimensionRecord<String> = createUnknown("UNKNOWN"),
    var goalRelevance: HealthDimensionRecord<RelevanceState> = createUnknown(RelevanceState.UNKNOWN)
)

fun <T> createUnknown(
    unknownValue: T, 
    reason: String = "Not inspected", 
    gap: CapabilityGap? = null
): HealthDimensionRecord<T> {
    return HealthDimensionRecord(
        value = unknownValue,
        confidence = 0.0,
        evidenceReferences = emptyList(),
        sourceCommitSha = null,
        uncertaintyReason = reason,
        capabilityGap = gap
    )
}
