package com.example.ai.capabilities.ecology

enum class SurfaceType {
    SOURCE,       // GitHub, Gitlab
    RUNTIME,      // Website, PWA, Android App
    DEVELOPMENT,  // Branches, Issues, CI/CD
    KNOWLEDGE,    // Documentation, Research, Videos
    EVIDENCE      // Tests, Builds, Observed Runtime Behavior
}

enum class InspectionStatus(val symbol: String) {
    UNREGISTERED("?"),
    DISCOVERED("○"),
    STRUCTURAL_INSPECTION_PENDING("○"),
    MAPPED("✓"),
    STALE("!")
}

data class RealitySurface(
    val id: String,
    val type: SurfaceType,
    val locationUri: String,
    var status: InspectionStatus = InspectionStatus.UNREGISTERED,
    var lastInspectedAt: Long = 0L,
    val knownFacts: MutableList<String> = mutableListOf()
)
