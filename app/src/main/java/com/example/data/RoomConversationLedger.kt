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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.ai.capabilities.ecology.RemoteControlPlaneRepository

/** Why the last canonical-sync attempt ended. Silence is not a status. */
enum class LedgerSyncOutcome { NEVER_ATTEMPTED, NOT_CONNECTED, FAILED, SYNCED }

/**
 * Observable result of canonical sync. Without this, a sync that can never run
 * is indistinguishable from one that works, because both are silent.
 */
data class LedgerSyncDiagnostic(
    val lastPush: LedgerSyncOutcome = LedgerSyncOutcome.NEVER_ATTEMPTED,
    val lastPull: LedgerSyncOutcome = LedgerSyncOutcome.NEVER_ATTEMPTED,
    val pushedEvents: Long = 0,
    val pulledEvents: Long = 0,
    val lastError: String? = null,
)

/** Marks rows that arrived from the control plane, and drives the pull cursor. */
internal const val SYNC_ORIGIN = "CANONICAL_SYNC"

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
    private val remoteSync: RemoteControlPlaneRepository = RemoteControlPlaneRepository.shared,
) : ImmutableConversationLedger {

    private val _syncDiagnostic = MutableStateFlow(LedgerSyncDiagnostic())

    /** Read this to tell a working sync from one that never ran. */
    val syncDiagnostic: StateFlow<LedgerSyncDiagnostic> = _syncDiagnostic.asStateFlow()

    init {
        scope.launch { syncFromCanonical() }
    }


    /**
     * Appends without blocking the caller. Chat send paths run on the main
     * dispatcher and must not stall on disk I/O, so the write is launched and
     * ordered by the single-threaded IO scope.
     */
    override fun appendEvent(event: ConversationEvent) {
        scope.launch { appendSuspending(event) }
    }

    /**
     * Use where the caller must observe the write, e.g. tests and migrations.
     *
     * The local append is the canonical act and completes on its own. The
     * remote push is launched separately and deliberately NOT awaited: this is
     * the single funnel every chat message passes through, and
     * `backfillLedgerFromMessages` drives it once per historical message on
     * launch. Awaiting a network round-trip here would put the control plane on
     * the critical path of Level 0 durability and turn a backfill of N messages
     * into N sequential requests.
     */
    suspend fun appendSuspending(event: ConversationEvent) {
        dao.append(event.toEntity())
        scope.launch { pushToCanonical(event) }
    }

    /** Suspending push, exposed so tests can observe the outcome. */
    suspend fun pushToCanonical(event: ConversationEvent) {
        val payload = mapOf(
            "eventId" to event.eventId,
            "timestamp" to event.timestamp,
            "actor" to event.actor.name,
            // The event's real provenance, not a hardcoded platform: a pushed
            // event that lies about its source cannot be reconciled later.
            "content" to event.rawContent,
            "source" to event.provenance.sourcePlatform.ifBlank { "ANDROID" },
            "conversationId" to event.provenance.conversationId,
        )
        remoteSync.syncConversationEvents(listOf(payload))
            .onSuccess { result ->
                _syncDiagnostic.value = _syncDiagnostic.value.copy(
                    lastPush = LedgerSyncOutcome.SYNCED,
                    pushedEvents = _syncDiagnostic.value.pushedEvents + 1,
                )
            }
            .onFailure { error -> recordFailure(error, push = true) }
    }

    /**
     * Pulls events the control plane has that this device does not.
     *
     * The cursor is the newest event that ARRIVED BY SYNC, not the newest event
     * overall. Using the overall newest — as the first version did via
     * `recentActive(1)` — races the local device against itself: every chat
     * message pushes the watermark past anything the control plane still holds,
     * so remote events older than your latest local message can never arrive.
     */
    suspend fun syncFromCanonical() {
        val since = dao.latestSyncedTimestamp(SYNC_ORIGIN) ?: 0L
        remoteSync.getConversationEvents(since)
            .onSuccess { list ->
                val entities = list.mapNotNull { ev ->
                    val id = ev["eventId"] as? String ?: return@mapNotNull null
                    ConversationEventEntity(
                        eventId = id,
                        timestamp = (ev["timestamp"] as? Number)?.toLong() ?: 0L,
                        actor = ev["actor"] as? String ?: "SYSTEM",
                        rawContent = ev["content"] as? String ?: "",
                        sourcePlatform = ev["source"] as? String ?: "UNKNOWN",
                        conversationId = ev["conversationId"] as? String ?: "default",
                        referencedArtifacts = "",
                        supersededByEventId = null,
                        // Records that this row came from the control plane, and
                        // is what the cursor above reads back.
                        migratedFrom = SYNC_ORIGIN,
                    )
                }
                // IGNORE on conflict makes a re-pull a no-op, so a reset cursor
                // costs bandwidth but can never duplicate history.
                val inserted = dao.appendAll(entities).count { it != -1L }
                _syncDiagnostic.value = _syncDiagnostic.value.copy(
                    lastPull = LedgerSyncOutcome.SYNCED,
                    pulledEvents = _syncDiagnostic.value.pulledEvents + inserted,
                )
            }
            .onFailure { error -> recordFailure(error, push = false) }
    }

    private fun recordFailure(error: Throwable, push: Boolean) {
        val outcome =
            if (error is com.example.ai.capabilities.ecology.NotConnectedException) {
                LedgerSyncOutcome.NOT_CONNECTED
            } else {
                LedgerSyncOutcome.FAILED
            }
        _syncDiagnostic.value = _syncDiagnostic.value.copy(
            lastPush = if (push) outcome else _syncDiagnostic.value.lastPush,
            lastPull = if (push) _syncDiagnostic.value.lastPull else outcome,
            lastError = error.message,
        )
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
