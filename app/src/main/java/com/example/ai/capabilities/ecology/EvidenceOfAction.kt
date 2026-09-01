package com.example.ai.capabilities.ecology

sealed class EvidenceOfAction {
    data class RepositoryObserved(
        val commitSha: String,
        val filesInspected: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    ) : EvidenceOfAction()

    data class BuildExecuted(
        val command: String,
        val exitCode: Int,
        val artifactHash: String?
    ) : EvidenceOfAction()

    data class ResearchPerformed(
        val sources: List<String>
    ) : EvidenceOfAction()

    data class ToolingAnomalyObserved(
        val event: String,
        val affectedThread: String,
        val buildTask: String,
        val artifactOutcome: String,
        val impactObserved: String,
        val epistemicStatus: String = "OBSERVED",
        val confidence: Double = 0.99,
        val falsificationCondition: String,
        val occurrenceCount: Int = 1,
        val timestamp: Long = System.currentTimeMillis()
    ) : EvidenceOfAction()

    data class CapabilityGapRecorded(
        val capabilityId: String,
        val requiredCapability: String,
        val whyRequired: String,
        val strategiesAttempted: List<String>,
        val authorizedAlternatives: List<String>,
        val estimatedCost: String,
        val securityImplications: String,
        val recommendedNextAcquisition: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : EvidenceOfAction()

    data class FederatedReconciliationEvent(
        val localEvidenceId: String,
        val remoteEvidenceId: String?,
        val origin: String,
        val timestamp: Long = System.currentTimeMillis(),
        val authorization: String,
        val commitOrEnvironmentId: String,
        val outcome: String,
        val details: String
    ) : EvidenceOfAction()
}
