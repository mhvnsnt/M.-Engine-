package com.example.ai.capabilities.multimodal

enum class SourceModality {
    VIDEO,
    AUDIO,
    TEXT_DOCUMENTATION,
    CODE_REPOSITORY,
    ACADEMIC_PAPER,
    LIVE_OBSERVATION
}

enum class PolicyCompliance {
    AUTHORIZED,
    RESTRICTED_METADATA_ONLY,
    UNAUTHORIZED_WAITING_FOR_CAPABILITY
}

data class ResearchArtifact(
    val id: String,
    val sourceUri: String,
    val modality: SourceModality,
    val timestamp: Long = System.currentTimeMillis(),
    val objectiveId: String,
    val extractedMechanics: List<String>,
    val observationVsInference: Map<String, String>, // Maps mechanic to "OBSERVED" or "INFERRED"
    val relatedArtifacts: List<String> = emptyList(), // Linking Video to GitHub code, etc.
    val complianceStatus: PolicyCompliance,
    // Mission 14 Metadata
    val acquisitionMethod: String = "UNKNOWN",
    val authorizationStatusAtAcquisition: String = "UNCERTAIN"
)

data class ResearchSynthesis(
    val summaryHypothesis: String,
    val correlatedArtifacts: List<ResearchArtifact>,
    val confidenceScore: Double,
    val falsificationCondition: String,
    val recommendedExperiment: String
)
