package com.example.ai.capabilities.memory

/**
 * MISSION 17.2E.2 — Context Reconstruction Engine
 * 
 * Bridges the permanent memory and the worker fabric by compiling a relevant,
 * epistemic-filtered, task-specific context based on the current explicit goals 
 * and immutable historical events.
 */
class ContextReconstructionEngine(
    private val ledger: ImmutableConversationLedger,
    private val ownerContext: OwnerContextGraph,
    private val ontologyFederation: OntologyFederationEngine
) {

    data class TaskContext(
        val relevantGoals: List<OwnerGoal>,
        val terminologyConstraints: List<TerminologyPreference>,
        val symbolicContext: List<OntologyClaim>,
        val historicalPrecedents: List<String>
    )

    /**
     * Compiles only the necessary context for an external worker to prevent overload.
     */
    fun compileTaskContext(taskCategory: String, targetOntologies: List<String> = emptyList()): TaskContext {
        // 1. Fetch relevant explicit goals
        val goals = ownerContext.getGoalsByCategory(taskCategory).ifEmpty { 
            // Fallback to searching all or general goals if specific ones aren't found
            // In a real system, this would perform a semantic similarity match.
            emptyList()
        }

        // 2. Terminology constraints come from the hydrated Owner Context Graph.
        // These used to be a hardcoded literal here, which meant the owner's
        // stated preference could only change by editing and recompiling the
        // app — and that the "preference" was really the developer's, not the
        // owner's. They are now data, loaded from persistent storage with
        // provenance.
        val constraints = ownerContext.allTerminologyPreferences()

        // 3. Filter Ontology Claims based on task domain
        val ownerName = ownerContext.identity?.identityFacts?.get("legalName")?.split(" ")?.firstOrNull() ?: "Owner"
        val claims = ontologyFederation.synthesizeInsights(ownerName)
            .filter { targetOntologies.contains(it.ontologyId) }

        // 4. Resolve supersession in history (naive simulation for probe)
        // Here we'd query the ledger for events matching the task and resolve them.
        // Only ACTIVE events are eligible: an event corrected by a later one
        // must not be replayed to a worker as though it still stood. This is
        // where supersession stops being bookkeeping and starts changing what
        // the system actually believes.
        val historicalPrecedents = mutableListOf<String>()
        val recentEvents = ledger.queryEventsByTime(0, System.currentTimeMillis())
        val activeEvents = recentEvents.filter { it.supersededByEventId == null }

        // Deliberately a small tail, not the whole ledger. Level 0 keeps
        // everything; the reconstructed context carries only what the task
        // needs, which is the entire point of reconstruction.
        activeEvents.takeLast(MAX_HISTORICAL_PRECEDENTS).forEach {
            historicalPrecedents.add("Prior Event [${it.actor}]: ${it.rawContent}")
        }

        return TaskContext(
            relevantGoals = goals,
            terminologyConstraints = constraints,
            symbolicContext = claims,
            historicalPrecedents = historicalPrecedents
        )
    }
    
    companion object {
        /** Keeps a long history from flooding a worker's context window. */
        const val MAX_HISTORICAL_PRECEDENTS = 3
    }

    fun resolveSupersession(eventId: String): ConversationEvent? {
        val chain = ledger.getProvenanceChain(eventId)
        return chain.lastOrNull() // The active event
    }
}
