package com.example.ai.capabilities.ecology

enum class ProjectHealth {
    HEALTHY, STALE, BROKEN, UNDERDEVELOPED, HIGH_POTENTIAL, RESEARCH_REQUIRED, ARCHIVED_CANDIDATE
}

data class ProjectEcologyProfile(
    val repositoryId: String,
    val structuralHealth: Float,
    val buildHealth: Float,
    val testHealth: Float,
    val dependencyFreshness: Float,
    val ownerGoalAlignment: Float,
    val leveragePotential: Float,
    val ecosystemConnectivity: Float,
    val researchPotential: Float,
    val maintenanceRisk: Float,
    val overallHealth: ProjectHealth,
    val lastObservedAt: Long,
    val physicalEvidence: EvidenceOfAction.RepositoryObserved? = null
)
