package com.example.ai.capabilities.ecology

enum class ReconciliationOutcome {
    CONFIRMED,
    MERGED,
    CONTESTED,
    SUPERSEDED,
    DUPLICATE,
    REJECTED
}

data class FederatedEvidenceRecord(
    val localEvidenceId: String,
    val remoteEvidenceId: String?,
    val origin: String,
    val timestamp: Long,
    val authorization: String,
    val commitOrEnvironmentId: String,
    val outcome: ReconciliationOutcome,
    val details: String
)

data class Contradiction(
    val subject: String,
    val claimA: String,
    val claimB: String,
    val possibleExplanations: List<String>,
    val revisedConfidence: Double,
    val nextAction: String
)

class EvidenceReconciliationEngine {
    
    fun reconcile(
        relationship: DependencyRelationship,
        documentaryEvidence: List<String>,
        structuralEvidence: List<String>
    ): Contradiction? {
        
        val claimsIntegration = documentaryEvidence.any { it.contains("integrates", ignoreCase = true) || it.contains("uses", ignoreCase = true) }
        val lacksStructure = structuralEvidence.isEmpty() || structuralEvidence.any { it.contains("no active integration", ignoreCase = true) || it.contains("no imports", ignoreCase = true) }

        if (claimsIntegration && lacksStructure) {
            // Downgrade the status of the relationship instead of discarding it
            relationship.status = EdgeStatus.CONTESTED
            
            // Calculate a penalized confidence score
            val revisedConfidence = relationship.confidence * 0.71 // e.g. 0.9 * 0.71 = ~0.64
            
            return Contradiction(
                subject = "${relationship.sourceId} → ${relationship.targetId}",
                claimA = "Documentary evidence indicates integration.",
                claimB = "Current structural inspection found no active integration.",
                possibleExplanations = listOf(
                    "1. Integration removed.",
                    "2. Documentation stale.",
                    "3. Integration occurs externally.",
                    "4. Inspection incomplete."
                ),
                revisedConfidence = revisedConfidence,
                nextAction = "Investigate before changing graph truth."
            )
        }
        
        return null
    }

    fun reconcileFederatedEvidence(
        localItem: EvidenceOfAction,
        remoteEntries: List<String>
    ): FederatedEvidenceRecord {
        val localId = localItem.hashCode().toString()
        val timestamp = System.currentTimeMillis()

        return when (localItem) {
            is EvidenceOfAction.RepositoryObserved -> {
                val matchesRemote = remoteEntries.any { it.contains(localItem.commitSha) }
                if (matchesRemote) {
                    FederatedEvidenceRecord(
                        localEvidenceId = localId,
                        remoteEvidenceId = "REMOTE_${localItem.commitSha.take(8)}",
                        origin = "EDGE_ANDROID",
                        timestamp = timestamp,
                        authorization = "AUTHORIZATION_L2",
                        commitOrEnvironmentId = localItem.commitSha,
                        outcome = ReconciliationOutcome.CONFIRMED,
                        details = "Edge observation matched remote ledger entry for commit ${localItem.commitSha}."
                    )
                } else {
                    FederatedEvidenceRecord(
                        localEvidenceId = localId,
                        remoteEvidenceId = null,
                        origin = "EDGE_ANDROID",
                        timestamp = timestamp,
                        authorization = "AUTHORIZATION_L2",
                        commitOrEnvironmentId = localItem.commitSha,
                        outcome = ReconciliationOutcome.MERGED,
                        details = "Edge observation merged into federated queue for remote sync."
                    )
                }
            }
            is EvidenceOfAction.ToolingAnomalyObserved -> {
                FederatedEvidenceRecord(
                    localEvidenceId = localId,
                    remoteEvidenceId = null,
                    origin = "TOOLING_OBSERVATORY",
                    timestamp = timestamp,
                    authorization = "AUTHORIZATION_SYSTEM",
                    commitOrEnvironmentId = localItem.buildTask,
                    outcome = ReconciliationOutcome.CONFIRMED,
                    details = "Observed tooling anomaly '${localItem.event}' logged on thread ${localItem.affectedThread}."
                )
            }
            is EvidenceOfAction.CapabilityGapRecorded -> {
                FederatedEvidenceRecord(
                    localEvidenceId = localId,
                    remoteEvidenceId = null,
                    origin = "CAPABILITY_HARVEST",
                    timestamp = timestamp,
                    authorization = "AUTHORIZATION_L3",
                    commitOrEnvironmentId = localItem.capabilityId,
                    outcome = ReconciliationOutcome.MERGED,
                    details = "Recorded capability gap '${localItem.requiredCapability}' with alternative strategies."
                )
            }
            is EvidenceOfAction.BuildExecuted -> {
                FederatedEvidenceRecord(
                    localEvidenceId = localId,
                    remoteEvidenceId = null,
                    origin = "BUILD_RUNNER",
                    timestamp = timestamp,
                    authorization = "AUTHORIZATION_L2",
                    commitOrEnvironmentId = localItem.command,
                    outcome = if (localItem.exitCode == 0) ReconciliationOutcome.CONFIRMED else ReconciliationOutcome.CONTESTED,
                    details = "Build task exit code ${localItem.exitCode} recorded."
                )
            }
            is EvidenceOfAction.ResearchPerformed -> {
                FederatedEvidenceRecord(
                    localEvidenceId = localId,
                    remoteEvidenceId = null,
                    origin = "RESEARCH_ENGINE",
                    timestamp = timestamp,
                    authorization = "AUTHORIZATION_L1",
                    commitOrEnvironmentId = localItem.sources.joinToString(","),
                    outcome = ReconciliationOutcome.CONFIRMED,
                    details = "Research evidence gathered from ${localItem.sources.size} sources."
                )
            }
            is EvidenceOfAction.FederatedReconciliationEvent -> {
                FederatedEvidenceRecord(
                    localEvidenceId = localItem.localEvidenceId,
                    remoteEvidenceId = localItem.remoteEvidenceId,
                    origin = localItem.origin,
                    timestamp = localItem.timestamp,
                    authorization = localItem.authorization,
                    commitOrEnvironmentId = localItem.commitOrEnvironmentId,
                    outcome = ReconciliationOutcome.valueOf(localItem.outcome),
                    details = localItem.details
                )
            }
        }
    }
    
    fun printContradiction(contradiction: Contradiction) {
        println("⚠️ CONTRADICTION DETECTED")
        println(contradiction.claimA)
        println(contradiction.claimB)
        println()
        println("Possible explanations:")
        contradiction.possibleExplanations.forEach { println(it) }
        println()
        println("Confidence:")
        println(String.format("%.2f", contradiction.revisedConfidence))
        println()
        println("NEXT ACTION:")
        println(contradiction.nextAction)
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
