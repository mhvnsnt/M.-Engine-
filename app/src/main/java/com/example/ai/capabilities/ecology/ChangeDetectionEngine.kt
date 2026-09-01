package com.example.ai.capabilities.ecology

class ChangeDetectionEngine {

    fun detectChanges(
        previousCommit: String?,
        newCommit: String?,
        modifiedFiles: List<String>,
        networkSuccess: Boolean = true
    ): ChangeDelta {
        if (!networkSuccess || newCommit == null) {
            return ChangeDelta(
                previousCommit = previousCommit,
                newCommit = newCommit,
                changes = emptyList(),
                successful = false,
                failureReason = "Network inspection failure or missing commit"
            )
        }

        if (previousCommit == newCommit) {
            return ChangeDelta(previousCommit, newCommit, emptyList())
        }

        val changes = modifiedFiles.map { file ->
            MeaningfulChange(
                fileModified = file,
                impact = classifyImpact(file),
                description = "File modified: $file"
            )
        }

        return ChangeDelta(previousCommit, newCommit, changes)
    }

    private fun classifyImpact(file: String): ChangeImpact {
        return when {
            file.endsWith(".md") || file.endsWith(".txt") -> ChangeImpact.DOCUMENTATION
            file.contains("test") || file.contains("spec") -> ChangeImpact.TEST
            file.contains("package.json") || file.contains("build.gradle") || file.contains("pom.xml") -> ChangeImpact.DEPENDENCY
            file.contains(".github/workflows") || file.contains("Dockerfile") -> ChangeImpact.BUILD_SYSTEM
            file.startsWith("src/") || file.endsWith(".kt") || file.endsWith(".ts") || file.endsWith(".js") -> ChangeImpact.SOURCE_STRUCTURE
            else -> ChangeImpact.UNKNOWN
        }
    }

    fun <T> transferEvidence(
        oldRecord: HealthDimensionRecord<T>,
        delta: ChangeDelta,
        dimensionName: String
    ): Pair<HealthDimensionRecord<T>, EvidenceTransferDecision> {
        
        // Mark old evidence as historical regardless of what happens
        oldRecord.evidenceStatus = EvidenceStatus.HISTORICAL
        
        if (!delta.successful || delta.newCommit == null) {
            // Cannot transfer to a blocked/unknown state
            val unknownRecord = oldRecord.copy(
                evidenceStatus = EvidenceStatus.CURRENT,
                sourceCommitSha = delta.newCommit,
                confidence = 0.0,
                uncertaintyReason = delta.failureReason ?: "Failed to inspect change delta"
            )
            return Pair(unknownRecord, EvidenceTransferDecision.NOT_TRANSFERABLE)
        }
        
        val impacts = delta.changes.map { it.impact }
        
        val decision = when (dimensionName) {
            "StructuralHealth" -> {
                if (impacts.contains(ChangeImpact.SOURCE_STRUCTURE) || impacts.contains(ChangeImpact.DEPENDENCY)) {
                    EvidenceTransferDecision.REQUIRES_REVALIDATION
                } else {
                    EvidenceTransferDecision.TRANSFERRED_WITH_HIGH_CONFIDENCE
                }
            }
            "BuildHealth" -> {
                if (impacts.isEmpty()) EvidenceTransferDecision.TRANSFERRED_WITH_HIGH_CONFIDENCE
                else EvidenceTransferDecision.REQUIRES_REVALIDATION // Any change requires physical rebuild for 100% truth
            }
            "DependencyFreshness" -> {
                if (impacts.contains(ChangeImpact.DEPENDENCY)) EvidenceTransferDecision.REQUIRES_REVALIDATION
                else EvidenceTransferDecision.TRANSFERRED_WITH_HIGH_CONFIDENCE
            }
            else -> EvidenceTransferDecision.REQUIRES_REVALIDATION
        }

        val newRecord = oldRecord.copy(
            sourceCommitSha = delta.newCommit,
            evidenceStatus = if (decision == EvidenceTransferDecision.REQUIRES_REVALIDATION) EvidenceStatus.STALE else EvidenceStatus.CURRENT,
            confidence = if (decision == EvidenceTransferDecision.REQUIRES_REVALIDATION) oldRecord.confidence * 0.5 else oldRecord.confidence,
            uncertaintyReason = if (decision == EvidenceTransferDecision.REQUIRES_REVALIDATION) "Stale evidence, requires revalidation on new commit" else oldRecord.uncertaintyReason
        )

        return Pair(newRecord, decision)
    }
}
