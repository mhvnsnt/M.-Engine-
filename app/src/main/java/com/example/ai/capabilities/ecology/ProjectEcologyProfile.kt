package com.example.ai.capabilities.ecology

data class ProjectEcologyProfile(
    val repositoryId: String,
    
    // Legacy simple scores, to be migrated away
    val structuralHealth: Float = 0f,
    val buildHealth: Float = 0f,
    val testHealth: Float = 0f,
    val dependencyFreshness: Float = 0f,
    val ownerGoalAlignment: Float = 0f,
    val leveragePotential: Float = 0f,
    val ecosystemConnectivity: Float = 0f,
    val researchPotential: Float = 0f,
    val maintenanceRisk: Float = 0f,
    val overallHealth: ProjectHealth = ProjectHealth.UNKNOWN,
    
    // New Epistemic Matrix
    var healthMatrix: HealthEvidenceMatrix = HealthEvidenceMatrix(),
    var inspectionState: InspectionState = InspectionState(),
    
    // Specific inspection snapshots
    var currentCommitSha: String? = null,
    var defaultBranch: String? = null,
    
    val lastObservedAt: Long = System.currentTimeMillis(),
    val physicalEvidence: EvidenceOfAction? = null
)
