package com.example.ai.capabilities

enum class InventoryState {
    ALREADY_EXISTS,
    PARTIALLY_EXISTS,
    BROKEN,
    EXPERIMENTAL,
    MISSING
}

enum class IntegrationMode {
    DIRECT_LIBRARY,
    SOURCE_ADAPTATION,
    MODEL,
    REMOTE_WORKER,
    ARCHITECTURAL_PATTERN,
    REPLACEMENT,
    REJECT
}

data class ProvenanceRecord(
    val originalRepo: String,
    val versionOrCommit: String,
    val license: String,
    val dependencies: List<String>,
    val securityConcerns: List<String>,
    val selectionReason: String,
    val replacedItem: String?,
    val benchmarks: String,
    val integrationStatus: String
)

data class ResearchCandidate(
    val id: String,
    val name: String,
    val sourceType: String, // GITHUB, PAPER, HUGGINGFACE, DOCS
    val url: String,
    val description: String,
    val versionOrCommit: String,
    val createdAtYear: Int = 2026,
    val lastUpdatedYear: Int = 2026,
    val stars: Int = 0,
    val forkCount: Int = 0,
    val issuesResolved: Int = 0,
    val isNativeMengine: Boolean = false
)

data class CandidateEvaluation(
    val effectivenessScore: Int,
    val efficiencyScore: Int,
    val maturityScore: Int,
    val recencyScore: Int,
    val adoptionScore: Int,
    val maintenanceScore: Int,
    val integrationComplexity: Int,
    val evidenceConfidence: String,
    val licenseCompatibility: Boolean,
    val androidCompatible: Boolean,
    val securityRisks: List<String>,
    val dependencyHealth: String,
    val recommendedIntegrationMode: IntegrationMode,
    val provenance: ProvenanceRecord? = null
)

data class ResearchRecommendation(
    val objective: String,
    val recommendedCandidate: ResearchCandidate?,
    val evaluation: CandidateEvaluation?,
    val alternatives: List<ResearchCandidate>,
    val reason: String
)

interface ResearchEngine {
    suspend fun discover(objective: String): List<ResearchCandidate>
    suspend fun evaluate(candidate: ResearchCandidate): CandidateEvaluation
    suspend fun compare(internalCapability: CapabilityInventoryItem?, candidates: List<ResearchCandidate>): ResearchRecommendation
    suspend fun proposeIntegration(recommendation: ResearchRecommendation): Boolean
}

data class CapabilityInventoryItem(
    val id: String,
    val name: String,
    val description: String,
    val state: InventoryState,
    val implementationRef: String?
)
