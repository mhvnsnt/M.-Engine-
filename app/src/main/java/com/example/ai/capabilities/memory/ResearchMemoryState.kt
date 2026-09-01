package com.example.ai.capabilities.memory

import com.example.ai.capabilities.multimodal.SourceModality

enum class ArtifactStatus {
    ACTIVE,
    STALE,
    NEEDS_VERIFICATION,
    SUPERSEDED
}

data class ArtifactLineage(
    val parentIds: List<String> = emptyList(),
    val descendantIds: List<String> = emptyList(),
    val derivedExperiments: List<String> = emptyList(),
    val contradictingArtifacts: List<String> = emptyList()
)

data class PersistentResearchArtifact(
    val artifactId: String,
    val objectiveId: String,
    val sourceUri: String,
    val sourceVersion: String?,
    val acquiredAt: Long,
    var lastVerifiedAt: Long,
    val modality: SourceModality,
    val observations: List<String>,
    val inferences: List<String>,
    val claims: List<String>,
    val evidence: List<String>,
    var confidence: Double,
    val assumptions: List<String>,
    val falsificationConditions: List<String>,
    var status: ArtifactStatus = ArtifactStatus.ACTIVE,
    val lineage: ArtifactLineage = ArtifactLineage(),
    val acquisitionMethod: String = "UNKNOWN",
    val authorizationStatusAtAcquisition: String = "UNCERTAIN"
)

data class MemoryDashboardStats(
    val totalArtifacts: Int,
    val active: Int,
    val stale: Int,
    val contested: Int,
    val superseded: Int,
    val currentlyRevalidating: Int,
    val mostRelevantToCurrentObjective: List<String>,
    val dormantKnowledgeReactivatedToday: Int,
    val beliefsRevisedToday: Int,
    val failedAssumptionsPreserved: Int
)
