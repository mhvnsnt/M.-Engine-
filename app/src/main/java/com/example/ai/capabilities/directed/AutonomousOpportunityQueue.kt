package com.example.ai.capabilities.directed

enum class OpportunityState {
    ACTIVE,
    RESEARCHING,
    MONITORING,
    WAITING,
    DEFERRED,
    NEW_OPPORTUNITY
}

data class QueuedOpportunity(
    val opportunity: Opportunity,
    var state: OpportunityState
)

interface AutonomousOpportunityQueue {
    fun enqueue(opportunity: Opportunity, state: OpportunityState = OpportunityState.NEW_OPPORTUNITY)
    fun updateState(opportunityId: String, newState: OpportunityState)
    fun getNextActive(): QueuedOpportunity?
    fun generateAgenda(): String
}

class AutonomousOpportunityQueueImpl : AutonomousOpportunityQueue {
    private val queue = mutableListOf<QueuedOpportunity>()

    override fun enqueue(opportunity: Opportunity, state: OpportunityState) {
        queue.add(QueuedOpportunity(opportunity, state))
    }

    override fun updateState(opportunityId: String, newState: OpportunityState) {
        queue.find { it.opportunity.id == opportunityId }?.state = newState
    }

    override fun getNextActive(): QueuedOpportunity? {
        return queue.firstOrNull { it.state == OpportunityState.NEW_OPPORTUNITY }
    }

    override fun generateAgenda(): String {
        val active = queue.filter { it.state == OpportunityState.ACTIVE }.map { it.opportunity.description }
        val researching = queue.filter { it.state == OpportunityState.RESEARCHING }.map { it.opportunity.description }
        val monitoring = queue.filter { it.state == OpportunityState.MONITORING }.map { it.opportunity.description }
        val waiting = queue.filter { it.state == OpportunityState.WAITING }.map { it.opportunity.description }
        val deferred = queue.filter { it.state == OpportunityState.DEFERRED }.map { it.opportunity.description }
        val newOps = queue.filter { it.state == OpportunityState.NEW_OPPORTUNITY }.map { it.opportunity.description }

        return """
            BACKGROUND AGENDA
            
            ACTIVE
            ${active.joinToString("\n") { "• $it" }}
            
            RESEARCHING
            ${researching.joinToString("\n") { "• $it" }}
            
            MONITORING
            ${monitoring.joinToString("\n") { "• $it" }}
            
            WAITING
            ${waiting.joinToString("\n") { "• $it" }}
            
            DEFERRED
            ${deferred.joinToString("\n") { "• $it" }}
            
            NEW OPPORTUNITY
            ${newOps.joinToString("\n") { "• $it" }}
        """.trimIndent()
    }
}
