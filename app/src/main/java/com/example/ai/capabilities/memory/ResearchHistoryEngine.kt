package com.example.ai.capabilities.memory

interface ResearchHistoryEngine {
    fun persistArtifact(artifact: PersistentResearchArtifact)
    fun reviseBelief(artifactId: String, newConfidence: Double, newClaims: List<String>, reason: String): PersistentResearchArtifact
    fun checkStaleness(currentTime: Long, thresholdMs: Long)
    fun reawakenDormantKnowledge(objectiveId: String, contextKeywords: List<String>): List<PersistentResearchArtifact>
    fun generateDashboard(): MemoryDashboardStats
    fun getArtifact(artifactId: String): PersistentResearchArtifact?
}

class ResearchHistoryEngineImpl(
    private val graph: SemanticResearchGraph = SemanticResearchGraphImpl(),
    private val independenceCheck: MemoryIndependenceCheck = MemoryIndependenceCheckImpl()
) : ResearchHistoryEngine {
    
    private val artifactHistory = mutableMapOf<String, MutableList<PersistentResearchArtifact>>()
    private val allArtifacts = mutableMapOf<String, PersistentResearchArtifact>()
    
    // Tracking metrics for dashboard
    private var reactivatedToday = 0
    private var revisedToday = 0
    private var failedAssumptions = mutableSetOf<String>()

    override fun persistArtifact(artifact: PersistentResearchArtifact) {
        val independence = independenceCheck.verifyIndependence(artifact, allArtifacts.values.toList())
        val finalArtifact = if (!independence.isIndependent) {
            // Cap confidence if not independent
            artifact.copy(confidence = artifact.confidence.coerceAtMost(0.5))
        } else {
            artifact
        }
        
        allArtifacts[finalArtifact.artifactId] = finalArtifact
        artifactHistory.getOrPut(finalArtifact.artifactId) { mutableListOf() }.add(finalArtifact)
        graph.addNode(finalArtifact)
        
        // Add failed assumptions to set
        failedAssumptions.addAll(finalArtifact.assumptions.filter { it.contains("FAILED") })
    }

    override fun reviseBelief(artifactId: String, newConfidence: Double, newClaims: List<String>, reason: String): PersistentResearchArtifact {
        val current = allArtifacts[artifactId] ?: throw IllegalArgumentException("Artifact not found")
        
        // Preserve history: do not overwrite, append new version
        val newVersion = current.copy(
            artifactId = "${artifactId}-v${artifactHistory[artifactId]!!.size + 1}",
            confidence = newConfidence,
            claims = newClaims,
            lastVerifiedAt = System.currentTimeMillis()
        )
        
        // Mark old version as SUPERSEDED, but keep it in history
        current.status = ArtifactStatus.SUPERSEDED
        
        allArtifacts[newVersion.artifactId] = newVersion
        artifactHistory[artifactId]!!.add(newVersion)
        graph.addNode(newVersion)
        graph.linkNodes(current.artifactId, newVersion.artifactId, "SUPERSEDED_BY")
        
        revisedToday++
        return newVersion
    }

    override fun checkStaleness(currentTime: Long, thresholdMs: Long) {
        allArtifacts.values.forEach { artifact ->
            if (artifact.status == ArtifactStatus.ACTIVE && (currentTime - artifact.lastVerifiedAt > thresholdMs)) {
                artifact.status = ArtifactStatus.STALE
            }
        }
    }

    override fun reawakenDormantKnowledge(objectiveId: String, contextKeywords: List<String>): List<PersistentResearchArtifact> {
        val found = graph.searchByRelevance(objectiveId, contextKeywords)
        val dormant = found.filter { it.status == ArtifactStatus.STALE || it.status == ArtifactStatus.NEEDS_VERIFICATION }
        
        dormant.forEach { 
            it.status = ArtifactStatus.NEEDS_VERIFICATION 
            reactivatedToday++
        }
        
        return dormant
    }

    override fun generateDashboard(): MemoryDashboardStats {
        val active = allArtifacts.values.count { it.status == ArtifactStatus.ACTIVE }
        val stale = allArtifacts.values.count { it.status == ArtifactStatus.STALE }
        val superseded = allArtifacts.values.count { it.status == ArtifactStatus.SUPERSEDED }
        val contested = allArtifacts.values.count { it.lineage.contradictingArtifacts.isNotEmpty() }
        val revalidating = allArtifacts.values.count { it.status == ArtifactStatus.NEEDS_VERIFICATION }
        
        val mostRelevant = allArtifacts.values
            .filter { it.status != ArtifactStatus.SUPERSEDED }
            .sortedByDescending { it.confidence }
            .take(3)
            .map { it.claims.firstOrNull() ?: it.artifactId }

        return MemoryDashboardStats(
            totalArtifacts = allArtifacts.size,
            active = active,
            stale = stale,
            contested = contested,
            superseded = superseded,
            currentlyRevalidating = revalidating,
            mostRelevantToCurrentObjective = mostRelevant,
            dormantKnowledgeReactivatedToday = reactivatedToday,
            beliefsRevisedToday = revisedToday,
            failedAssumptionsPreserved = failedAssumptions.size
        )
    }

    override fun getArtifact(artifactId: String): PersistentResearchArtifact? {
        return allArtifacts[artifactId]
    }
}
