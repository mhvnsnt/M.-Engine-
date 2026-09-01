package com.example.ai.capabilities.ecology

class ReinspectionPlanner {

    fun planReinspection(
        delta: ChangeDelta,
        relevanceScore: Double = 1.0,
        currentUncertainty: Double = 0.5
    ): ReinspectionAction {
        if (!delta.successful) {
            return ReinspectionAction.NO_ACTION // Can't reinspect if network is down
        }
        
        if (delta.changes.isEmpty()) {
            return ReinspectionAction.NO_ACTION
        }

        val maxImpact = delta.changes.maxOfOrNull { impactScore(it.impact) } ?: 0.0
        val consequence = if (delta.changes.size > 10) 0.8 else 0.4
        val verificationCost = 0.2 // Simplified for mock
        
        val priority = (relevanceScore * maxImpact * consequence * currentUncertainty) / verificationCost

        return when {
            priority > 1.5 -> ReinspectionAction.FULL_REINSPECTION
            priority > 0.8 -> ReinspectionAction.TARGETED_REINSPECTION
            priority > 0.3 -> ReinspectionAction.LIGHTWEIGHT_RECHECK
            else -> ReinspectionAction.NO_ACTION
        }
    }

    private fun impactScore(impact: ChangeImpact): Double {
        return when (impact) {
            ChangeImpact.SECURITY_RELEVANT -> 1.0
            ChangeImpact.DEPENDENCY -> 0.9
            ChangeImpact.BUILD_SYSTEM -> 0.8
            ChangeImpact.SOURCE_STRUCTURE -> 0.7
            ChangeImpact.RUNTIME -> 0.7
            ChangeImpact.TEST -> 0.4
            ChangeImpact.DOCUMENTATION -> 0.1
            ChangeImpact.COSMETIC -> 0.05
            ChangeImpact.UNKNOWN -> 0.5
        }
    }
}
