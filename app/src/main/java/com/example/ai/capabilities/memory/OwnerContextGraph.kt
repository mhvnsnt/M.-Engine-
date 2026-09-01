package com.example.ai.capabilities.memory

import com.example.ai.capabilities.ecology.PhysicalOwnerIdentity

/**
 * MISSION 17.2E.3 — Owner Context Graph
 * 
 * Defines the structured graph of explicit owner facts, goals, preferences, 
 * and constraints.
 */

data class OwnerGoal(
    val id: String,
    val description: String,
    val category: String, // e.g., "SECURITY", "ABUNDANCE", "INFLUENCE"
    val timeHorizon: String,
    val priority: Int
)

data class TerminologyPreference(
    val rejectedTerm: String,
    val preferredTerm: String,
    val context: String
)

class OwnerContextGraph(private val ontologyEngine: OntologyFederationEngine) {
    
    var identity: PhysicalOwnerIdentity? = null
        private set

    private val goals = mutableListOf<OwnerGoal>()
    private val terminologyPreferences = mutableListOf<TerminologyPreference>()

    fun hydrate(
        newIdentity: PhysicalOwnerIdentity,
        newGoals: List<OwnerGoal>,
        newPreferences: List<TerminologyPreference>,
        symbolicClaims: List<OntologyClaim> = emptyList()
    ) {
        this.identity = newIdentity
        this.goals.clear()
        this.goals.addAll(newGoals)
        
        this.terminologyPreferences.clear()
        this.terminologyPreferences.addAll(newPreferences)
        
        symbolicClaims.forEach { ontologyEngine.addClaim(it) }
    }

    fun getGoalsByCategory(category: String): List<OwnerGoal> = goals.filter { it.category == category }
    
    /** All hydrated preferences. Empty until [hydrate] has run. */
    fun allTerminologyPreferences(): List<TerminologyPreference> = terminologyPreferences.toList()

    /** All hydrated goals, regardless of category. */
    fun allGoals(): List<OwnerGoal> = goals.toList()

    fun getPreferredTerminology(term: String): String? {
        return terminologyPreferences.find { it.rejectedTerm.contains(term, ignoreCase = true) }?.preferredTerm
    }
}
