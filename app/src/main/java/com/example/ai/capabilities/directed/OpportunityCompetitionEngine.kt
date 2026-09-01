package com.example.ai.capabilities.directed

import com.example.ai.capabilities.ecology.ProjectEcologyEngine

data class EvaluatedOpportunity(
    val title: String,
    val targetProjectId: String,
    val ownerGoalAlignment: Double,
    val expectedLeverage: Double,
    val evidenceConfidence: Double,
    val crossProjectImpact: Double,
    val reversibility: Double,
    val cost: Double,
    val risk: Double,
    val opportunityCost: Double,
    val evidence: String
) {
    val opportunityScore: Double
        get() = (ownerGoalAlignment * expectedLeverage * evidenceConfidence * crossProjectImpact * reversibility) / (cost + risk + opportunityCost)
}

class OpportunityCompetitionEngine(private val ecologyEngine: ProjectEcologyEngine) {
    
    fun evaluateOpportunities(candidates: List<EvaluatedOpportunity>): List<EvaluatedOpportunity> {
        println("━━━━━━━━ M. ENGINE — OPPORTUNITY COMPETITION ━━━━━━━━")
        println("Evaluating ${candidates.size} evidence-backed opportunities across the ecosystem.")
        println()
        
        val ranked = candidates.sortedByDescending { it.opportunityScore }
        
        ranked.forEachIndexed { index, opp ->
            println("${index + 1}. [Score: ${String.format("%.2f", opp.opportunityScore)}] ${opp.title}")
            println("   Target: ${opp.targetProjectId}")
            println("   Evidence: ${opp.evidence}")
            println("   Metrics: Alignment=${opp.ownerGoalAlignment}, Leverage=${opp.expectedLeverage}, Confidence=${opp.evidenceConfidence}, Impact=${opp.crossProjectImpact}")
            println("   Burdens: Cost=${opp.cost}, Risk=${opp.risk}, OpportunityCost=${opp.opportunityCost}")
            println()
        }
        
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return ranked
    }
}
