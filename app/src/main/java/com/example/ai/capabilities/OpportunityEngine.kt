package com.example.ai.capabilities

data class Opportunity(
    val id: String,
    val title: String,
    val description: String,
    val marketPain: Double, // 1.0 to 10.0
    val technicalFeasibility: Double, // 1.0 to 10.0
    val differentiation: Double, // 1.0 to 10.0
    val distribution: Double, // 1.0 to 10.0
    val revenuePotential: Double, // 1.0 to 10.0
    val timing: Double, // 1.0 to 10.0
    val costRisk: Double // 1.0 to 10.0 (Higher means riskier/more costly)
) {
    val opportunityScore: Double
        get() = (marketPain * technicalFeasibility * differentiation * distribution * revenuePotential * timing) / costRisk
}

interface OpportunityEngine {
    fun evaluateOpportunity(opportunity: Opportunity): Double
    fun rankOpportunities(opportunities: List<Opportunity>): List<Opportunity>
}

class OpportunityEngineImpl : OpportunityEngine {
    override fun evaluateOpportunity(opportunity: Opportunity): Double {
        return opportunity.opportunityScore
    }

    override fun rankOpportunities(opportunities: List<Opportunity>): List<Opportunity> {
        return opportunities.sortedByDescending { it.opportunityScore }
    }
}
