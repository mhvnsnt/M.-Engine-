package com.example.ai.capabilities.ecology

enum class ChangeImpact { 
    COSMETIC, DOCUMENTATION, TEST, DEPENDENCY, BUILD_SYSTEM, SOURCE_STRUCTURE, RUNTIME, SECURITY_RELEVANT, UNKNOWN 
}

enum class EvidenceStatus { 
    CURRENT, HISTORICAL, STALE, CONTESTED, SUPERSEDED, EXPIRED, REQUIRES_REVALIDATION 
}

enum class EvidenceTransferDecision { 
    TRANSFERRED_WITH_HIGH_CONFIDENCE, TRANSFERRED_WITH_LIMITATION, REQUIRES_REVALIDATION, NOT_TRANSFERABLE 
}

enum class ReinspectionAction { 
    NO_ACTION, LIGHTWEIGHT_RECHECK, TARGETED_REINSPECTION, FULL_REINSPECTION, ACQUIRE_CAPABILITY 
}

data class MeaningfulChange(
    val fileModified: String,
    val impact: ChangeImpact,
    val description: String
)

data class ChangeDelta(
    val previousCommit: String?,
    val newCommit: String?,
    val changes: List<MeaningfulChange>,
    val successful: Boolean = true,
    val failureReason: String? = null
)
