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
}
