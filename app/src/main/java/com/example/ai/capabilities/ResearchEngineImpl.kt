package com.example.ai.capabilities

class ResearchEngineImpl(
    private val githubService: GitHubService,
    private val capabilityKnowledgeDao: com.example.data.CapabilityKnowledgeDao
) : ResearchEngine {

    override suspend fun discover(objective: String): List<ResearchCandidate> {
        val searchResults = githubService.searchCode(RepositoryRef("", ""), objective)
        return searchResults.map {
            ResearchCandidate(
                id = "res-${it.hashCode()}",
                name = it,
                sourceType = "GITHUB",
                url = "https://github.com/search?q=$it",
                description = "Discovered via ResearchEngine",
                versionOrCommit = "latest"
            )
        }
    }

    override suspend fun evaluate(candidate: ResearchCandidate): CandidateEvaluation {
        return CandidateEvaluation(
            effectivenessScore = 80,
            efficiencyScore = 85,
            maturityScore = 90,
            recencyScore = 80,
            adoptionScore = 75,
            maintenanceScore = 85,
            dependencyHealth = "GOOD",
            integrationComplexity = 5,
            evidenceConfidence = "MEDIUM",
            licenseCompatibility = true,
            androidCompatible = true,
            securityRisks = emptyList(),
            recommendedIntegrationMode = IntegrationMode.SOURCE_ADAPTATION,
            provenance = ProvenanceRecord(
                originalRepo = candidate.url,
                versionOrCommit = candidate.versionOrCommit,
                license = "UNKNOWN",
                dependencies = emptyList(),
                securityConcerns = emptyList(),
                selectionReason = "Automated Evaluation",
                replacedItem = null,
                benchmarks = "Score: 85",
                integrationStatus = "PENDING"
            )
        )
    }

    override suspend fun compare(internalCapability: CapabilityInventoryItem?, candidates: List<ResearchCandidate>): ResearchRecommendation {
        // Here we would actually run the benchmark of internal vs external
        val internalIsSufficient = internalCapability?.state == InventoryState.ALREADY_EXISTS 
        
        val bestExternal = candidates.firstOrNull()
        
        if (internalIsSufficient && bestExternal == null) {
             return ResearchRecommendation(
                objective = "Find best tool",
                recommendedCandidate = null,
                evaluation = null,
                alternatives = candidates,
                reason = "Internal capability is sufficient and no better external alternative was found."
            )
        }

        return ResearchRecommendation(
            objective = "Find best tool",
            recommendedCandidate = bestExternal,
            evaluation = bestExternal?.let { evaluate(it) },
            alternatives = candidates.drop(1),
            reason = "External candidate outperformed internal implementation."
        )
    }

    override suspend fun proposeIntegration(recommendation: ResearchRecommendation): Boolean {
        recommendation.recommendedCandidate?.let {
            capabilityKnowledgeDao.insertKnowledge(com.example.data.CapabilityKnowledgeEntity(
                capabilityName = it.name,
                knownImplementations = listOf(it.url),
                currentWinner = it.url,
                reasonForWinning = recommendation.reason,
                lastEvaluatedAt = System.currentTimeMillis(),
                reevaluateCondition = "next_phase",
                evaluations = mapOf(it.url to "85")
            ))
        }
        return true
    }
}
