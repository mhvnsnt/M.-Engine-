package com.example.ai.capabilities.directed

data class Opportunity(
    val id: String,
    val description: String,
    val alignedObjectives: List<String>,
    val expectedLeverage: Double,
    val reversibility: Double,
    val evidenceConfidence: Double,
    val cost: Double,
    val risk: Double,
    val attentionConsumption: Double
)

interface DirectedAgencyEngine {
    fun evaluateOpportunity(opportunity: Opportunity, graph: OwnerObjectiveGraph): Double
    fun selectNextBestAction(opportunities: List<Opportunity>, graph: OwnerObjectiveGraph): Opportunity?
}

class DirectedAgencyEngineImpl : DirectedAgencyEngine {
    override fun evaluateOpportunity(opportunity: Opportunity, graph: OwnerObjectiveGraph): Double {
        // Goal Alignment based on related objectives
        val activeObjectives = graph.getAllObjectives()
        val alignmentScore = opportunity.alignedObjectives.mapNotNull { objId ->
            activeObjectives.find { it.id == objId }?.weight
        }.average().takeIf { !it.isNaN() } ?: 0.5

        // Directed Value Formula
        // (Goal Alignment × Expected Leverage × Reversibility × Evidence Confidence) / (Cost + Risk + Attention Consumption)
        val numerator = alignmentScore * opportunity.expectedLeverage * opportunity.reversibility * opportunity.evidenceConfidence
        val denominator = opportunity.cost + opportunity.risk + opportunity.attentionConsumption

        return if (denominator > 0) numerator / denominator else 0.0
    }

    override fun selectNextBestAction(opportunities: List<Opportunity>, graph: OwnerObjectiveGraph): Opportunity? {
        return opportunities.maxByOrNull { evaluateOpportunity(it, graph) }
    }
}
