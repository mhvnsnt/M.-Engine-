package com.example.ai.capabilities.memory

import java.io.Serializable
import java.util.UUID

/**
 * MISSION 17.2E.1 — Immutable Total Conversation Ledger
 *
 * This is the raw immutable ledger of all interactions. Summaries never replace
 * these records; they only point back to them for provenance.
 */

enum class ConversationActor : Serializable {
    OWNER,
    M_ENGINE,
    WORKER,
    SYSTEM
}

data class EventProvenance(
    val sourcePlatform: String, // e.g., "AI_STUDIO", "PWA", "GITHUB"
    val conversationId: String,
    val referencedArtifacts: List<String> = emptyList()
) : Serializable

data class ConversationEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actor: ConversationActor,
    val rawContent: String,
    val provenance: EventProvenance,
    val supersededByEventId: String? = null // Tracking corrections over time
) : Serializable

interface ImmutableConversationLedger {
    fun appendEvent(event: ConversationEvent)
    fun getEvent(eventId: String): ConversationEvent?
    fun queryEventsByTime(startTime: Long, endTime: Long): List<ConversationEvent>
    fun getProvenanceChain(eventId: String): List<ConversationEvent>
}

class InMemoryConversationLedger : ImmutableConversationLedger {
    private val ledger = mutableListOf<ConversationEvent>()
    private val indexById = mutableMapOf<String, ConversationEvent>()

    override fun appendEvent(event: ConversationEvent) {
        ledger.add(event)
        indexById[event.eventId] = event
    }

    override fun getEvent(eventId: String): ConversationEvent? = indexById[eventId]

    override fun queryEventsByTime(startTime: Long, endTime: Long): List<ConversationEvent> {
        return ledger.filter { it.timestamp in startTime..endTime }
    }

    override fun getProvenanceChain(eventId: String): List<ConversationEvent> {
        val chain = mutableListOf<ConversationEvent>()
        var current = getEvent(eventId)
        while (current != null) {
            chain.add(current)
            current = current.supersededByEventId?.let { getEvent(it) }
        }
        return chain
    }
}
