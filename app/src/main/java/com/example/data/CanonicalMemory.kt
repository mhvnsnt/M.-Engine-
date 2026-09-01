package com.example.data

import com.example.ai.capabilities.ecology.PhysicalOwnerIdentity
import com.example.ai.capabilities.memory.ContextReconstructionEngine
import com.example.ai.capabilities.memory.OntologyFederationEngine
import com.example.ai.capabilities.memory.OwnerContextGraph
import com.example.ai.capabilities.memory.OwnerGoal
import com.example.ai.capabilities.memory.TerminologyPreference

/**
 * Assembles the canonical memory pipeline and hydrates it from persistent
 * storage.
 *
 * This is the connection the memory package never had. Every one of these types
 * was written, compiled and unreachable: a reachability audit found the entire
 * `ai/capabilities/memory` package unreferenced from any entry point, which
 * meant M. Engine's stated architecture could not execute while the product ran
 * its conversations through a separate Room path.
 *
 * Levels, per the canonical hierarchy:
 *   Level 0  ImmutableConversationLedger  (RoomConversationLedger)
 *   Level 1  Owner context                (OwnerContextGraph, hydrated here)
 *   Level 3  Ontology claims              (OntologyFederationEngine)
 *   Level 5  Context reconstruction       (ContextReconstructionEngine)
 *
 * Levels 2, 4 and 6 (project memory, semantic retrieval, meta-memory) are NOT
 * assembled here and are not claimed to exist.
 */
class CanonicalMemory(
    val ledger: RoomConversationLedger,
    private val ownerContextDao: OwnerContextDao,
) {
    val ontology = OntologyFederationEngine()
    val ownerContext = OwnerContextGraph(ontology)
    val contextReconstruction = ContextReconstructionEngine(ledger, ownerContext, ontology)

    /**
     * Loads owner goals and terminology preferences from the database into the
     * in-memory graph.
     *
     * Only ACTIVE rows are hydrated. A superseded preference stays in the table
     * for provenance but must not reach a worker as though it still stood —
     * that distinction is the whole reason supersession exists rather than
     * UPDATE-in-place.
     *
     * Returns the number of preferences hydrated, so a caller can tell the
     * difference between "no preferences" and "hydration never ran".
     */
    suspend fun hydrate(identity: PhysicalOwnerIdentity? = null): Int {
        val goals = ownerContextDao.activeGoals().map {
            OwnerGoal(
                id = it.id,
                description = it.description,
                category = it.category,
                timeHorizon = it.timeHorizon,
                priority = it.priority,
            )
        }
        val prefs = ownerContextDao.activePreferences().map {
            TerminologyPreference(
                rejectedTerm = it.rejectedTerm,
                preferredTerm = it.preferredTerm,
                context = it.context,
            )
        }

        // hydrate() requires a non-null identity; an empty one is used when the
        // owner identity has not been established, so terminology and goals
        // still load rather than the whole graph staying dark.
        ownerContext.hydrate(
            newIdentity = identity ?: EMPTY_IDENTITY,
            newGoals = goals,
            newPreferences = prefs,
        )
        return prefs.size
    }

    /**
     * Records a correction to a terminology preference WITHOUT destroying the
     * superseded row, then re-hydrates so the running system reflects it.
     */
    suspend fun supersedePreference(oldTerm: String, replacement: TerminologyPreferenceEntity) {
        ownerContextDao.upsertPreference(replacement)
        ownerContextDao.supersedePreference(oldTerm, replacement.rejectedTerm)
        hydrate()
    }

    /**
     * Seeds the owner's standing terminology preference the first time only.
     *
     * This preference previously lived as a Kotlin literal inside
     * ContextReconstructionEngine. Seeding it as a row keeps the same operating
     * language in force while making it editable, provenance-bearing and
     * supersedable — the point being that it is now the owner's data rather
     * than the developer's constant.
     */
    suspend fun seedDefaultsIfEmpty(): Boolean {
        if (ownerContextDao.preferenceCount() > 0) return false
        ownerContextDao.upsertPreference(
            TerminologyPreferenceEntity(
                rejectedTerm = "Sovereignty",
                preferredTerm = "Agentic Autonomy",
                context = "M. Engine Operating Philosophy",
                provenance = "EXPLICIT",
            ),
        )
        return true
    }

    private companion object {
        /**
         * Used when no owner identity has been established yet. Empty rather
         * than invented: fabricating owner facts to satisfy a non-null
         * parameter would put guesses into Level 1 memory.
         */
        val EMPTY_IDENTITY = PhysicalOwnerIdentity(
            identityFacts = emptyMap(),
            verifiedAttributes = emptySet(),
            geographicAnchors = emptyList(),
            explicitPreferences = emptyMap(),
        )
    }
}
