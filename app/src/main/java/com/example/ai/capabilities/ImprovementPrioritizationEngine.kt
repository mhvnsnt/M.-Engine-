package com.example.ai.capabilities

/**
 * Value-Prioritization Engine for Autonomous Self-Development:
 *
 * Prevents autonomous development from devolving into endless refactoring churn.
 * Implements the explicit utility function:
 *
 *   ValueScore = (Impact × Confidence × Feasibility × EvidenceQuality × UserValue)
 *                - (Risk × Complexity × RegressionPotential)
 */

data class ImprovementCandidate(
    val id: String,
    val title: String,
    val componentTarget: String,
    val description: String,
    val impact: Double,            // 1.0 to 10.0
    val confidence: Double,        // 0.0 to 1.0
    val feasibility: Double,       // 0.0 to 1.0
    val evidenceQuality: Double,   // 0.0 to 1.0
    val userValue: Double,         // 1.0 to 10.0
    val risk: Double,              // 0.0 to 5.0
    val complexity: Double,        // 0.0 to 5.0
    val regressionPotential: Double, // 0.0 to 5.0
    val externalHardwareRequired: Boolean = false,
    val missingCredentialsRequired: Boolean = false,
    val category: String = "CORE_SYSTEM"
)

data class ScoredImprovementCandidate(
    val candidate: ImprovementCandidate,
    val rawValueScore: Double,
    val isExecutableInCurrentEnvironment: Boolean,
    val boundaryClassification: String,
    val prioritizationRankingReason: String
)

interface ImprovementPrioritizationEngine {
    fun scoreCandidate(candidate: ImprovementCandidate): ScoredImprovementCandidate
    fun rankCandidates(candidates: List<ImprovementCandidate>): List<ScoredImprovementCandidate>
    fun selectHighestValueExecutableCandidate(candidates: List<ImprovementCandidate>): ScoredImprovementCandidate?
}

class ImprovementPrioritizationEngineImpl : ImprovementPrioritizationEngine {

    override fun scoreCandidate(candidate: ImprovementCandidate): ScoredImprovementCandidate {
        // Enforce Reality Boundary: If unavailable external hardware or cloud secrets are required,
        // feasibility drops to 0.0 and execution is blocked. Never convert into simulation.
        val isBlockedByBoundary = candidate.externalHardwareRequired || candidate.missingCredentialsRequired
        val effectiveFeasibility = if (isBlockedByBoundary) 0.0 else candidate.feasibility.coerceIn(0.0, 1.0)

        val positiveUtility = (candidate.impact.coerceIn(1.0, 10.0)) *
                (candidate.confidence.coerceIn(0.0, 1.0)) *
                effectiveFeasibility *
                (candidate.evidenceQuality.coerceIn(0.0, 1.0)) *
                (candidate.userValue.coerceIn(1.0, 10.0))

        val riskPenalty = (candidate.risk.coerceIn(0.0, 5.0)) *
                (candidate.complexity.coerceIn(0.0, 5.0)) *
                (candidate.regressionPotential.coerceIn(0.0, 5.0))

        val netValueScore = positiveUtility - riskPenalty

        val boundaryClassification = when {
            candidate.externalHardwareRequired -> "BLOCKED_PHYSICAL_HARDWARE"
            candidate.missingCredentialsRequired -> "BLOCKED_MISSING_CREDENTIALS"
            effectiveFeasibility >= 0.8 -> "FULLY_LOCAL_EXECUTABLE"
            else -> "PARTIALLY_CONSTRAINED"
        }

        val reason = if (isBlockedByBoundary) {
            "BLOCKED: Candidate requires unavailable physical hardware/credentials. Reality contract prohibits synthetic simulation."
        } else {
            "Value Score: ${String.format("%.2f", netValueScore)} (Utility: ${String.format("%.2f", positiveUtility)} - Risk Penalty: ${String.format("%.2f", riskPenalty)})"
        }

        return ScoredImprovementCandidate(
            candidate = candidate,
            rawValueScore = netValueScore,
            isExecutableInCurrentEnvironment = !isBlockedByBoundary && effectiveFeasibility > 0.0,
            boundaryClassification = boundaryClassification,
            prioritizationRankingReason = reason
        )
    }

    override fun rankCandidates(candidates: List<ImprovementCandidate>): List<ScoredImprovementCandidate> {
        return candidates
            .map { scoreCandidate(it) }
            .sortedWith(
                compareByDescending<ScoredImprovementCandidate> { it.isExecutableInCurrentEnvironment }
                    .thenByDescending { it.rawValueScore }
            )
    }

    override fun selectHighestValueExecutableCandidate(candidates: List<ImprovementCandidate>): ScoredImprovementCandidate? {
        return rankCandidates(candidates).firstOrNull { it.isExecutableInCurrentEnvironment && it.rawValueScore > 0 }
    }
}
