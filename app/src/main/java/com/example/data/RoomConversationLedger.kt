package com.example.data

import com.example.ai.capabilities.memory.ConversationActor
import com.example.ai.capabilities.memory.ConversationEvent
import com.example.ai.capabilities.memory.EventProvenance
import com.example.ai.capabilities.memory.ImmutableConversationLedger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * The canonical Level 0 authority: `ImmutableConversationLedger` backed by the
 * application's encrypted Room database.
 *
 * Why Room rather than the existing `FileBackedConversationLedger`: the app's
 * database is opened through SQLCipher, so history stays encrypted at rest, and
 * using the same store means there is exactly ONE persistence authority instead
 * of a file ledger racing a Room `messages` table.
 *
 * Relationship to the `messages` table: `messages` remains the UI read model —
 * a projection, not an authority. This ledger is what any later derived memory,
 * summary or knowledge claim must trace back to.
 *
 * The interface it implements is synchronous while Room is suspending, so
 * blocking bridges are used deliberately and are documented per method.
 */
class RoomConversationLedger(
    private val dao: ConversationEventDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ImmutableConversationLedger {

    /**
     * Appends without blocking the caller. Chat send paths run on the main
     * dispatcher and must not stall on disk I/O, so the write is launched and
     * ordered by the single-threaded IO scope.
     */
    override fun appendEvent(event: ConversationEvent) {
        scope.launch { appendSuspending(event) }
    }

    /** Use where the caller must observe the write, e.g. tests and migrations. */
    suspend fun appendSuspending(event: ConversationEvent) {
        dao.append(event.toEntity())
    }

    override fun getEvent(eventId: String): ConversationEvent? =
        runBlocking { dao.getEvent(eventId)?.toDomain() }

    override fun queryEventsByTime(startTime: Long, endTime: Long): List<ConversationEvent> =
        runBlocking { dao.queryByTime(startTime, endTime).map { it.toDomain() } }

    /**
     * Walks the supersession chain from an event to its currently active
     * successor. The original is never removed; the chain is how a correction
     * stays auditable.
     */
    override fun getProvenanceChain(eventId: String): List<ConversationEvent> = runBlocking {
        val chain = mutableListOf<ConversationEvent>()
        val seen = mutableSetOf<String>()
        var current = dao.getEvent(eventId)
        // A malformed chain must not hang the app, so cycles terminate the walk.
        while (current != null && seen.add(current.eventId)) {
            chain.add(current.toDomain())
            current = current.supersededByEventId?.let { dao.getEvent(it) }
        }
        chain
    }

    /** Records that [eventId] has been corrected by [successorId]. */
    suspend fun supersede(eventId: String, successorId: String) {
        dao.markSuperseded(eventId, successorId)
    }

    suspend fun recentActive(limit: Int): List<ConversationEvent> =
        dao.recentActive(limit).map { it.toDomain() }

    suspend fun count(): Int = dao.count()
}

internal fun ConversationEvent.toEntity(migratedFrom: String? = null) = ConversationEventEntity(
    eventId = eventId,
    timestamp = timestamp,
    actor = actor.name,
    rawContent = rawContent,
    sourcePlatform = provenance.sourcePlatform,
    conversationId = provenance.conversationId,
    referencedArtifacts = provenance.referencedArtifacts.joinToString(","),
    supersededByEventId = supersededByEventId,
    migratedFrom = migratedFrom,
)

internal fun ConversationEventEntity.toDomain() = ConversationEvent(
    eventId = eventId,
    timestamp = timestamp,
    // An unrecognised actor becomes SYSTEM rather than throwing: a single bad
    // row must never make the whole history unreadable.
    actor = runCatching { ConversationActor.valueOf(actor) }.getOrDefault(ConversationActor.SYSTEM),
    rawContent = rawContent,
    provenance = EventProvenance(
        sourcePlatform = sourcePlatform,
        conversationId = conversationId,
        referencedArtifacts = referencedArtifacts.split(",").filter { it.isNotBlank() },
    ),
    supersededByEventId = supersededByEventId,
)
