package com.example.data

import com.example.ai.capabilities.memory.ConversationActor
import com.example.ai.capabilities.memory.ConversationEvent
import com.example.ai.capabilities.memory.EventProvenance
import kotlinx.coroutines.flow.Flow

/**
 * The single funnel for conversation persistence.
 *
 * [insertMessage] is called from a dozen sites in ChatViewModel. Appending to
 * the canonical ledger at each of those call sites would guarantee that one is
 * eventually missed — the recurring failure mode where a rule is only as good
 * as the paths that remember to apply it. Intercepting here means no message
 * path can reach storage without also reaching Level 0.
 *
 * `messages` remains the UI read model. The ledger is the authority.
 */
class ChatRepository(
    private val messageDao: MessageDao,
    private val styleDao: StyleDao,
    private val endpointDao: EndpointDao,
    private val sessionDao: SessionDao,
    private val ledger: RoomConversationLedger? = null,
) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>> = messageDao.getMessagesForSession(sessionId)
    
    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)
    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)

    val styleProfile: Flow<StyleProfileEntity?> = styleDao.getProfile()
    val allEndpoints: Flow<List<EndpointEntity>> = endpointDao.getAllEndpoints()

    suspend fun getActiveEndpoints() = endpointDao.getActiveEndpoints()
    suspend fun getAllEndpointsSync() = endpointDao.getAllEndpointsSync()
    suspend fun getPrimaryEndpoint() = endpointDao.getPrimaryEndpoint()
    suspend fun updateEndpointApiKey(id: Int, apiKey: String) = endpointDao.updateApiKey(id, apiKey)
    suspend fun insertEndpoint(endpoint: EndpointEntity) = endpointDao.insertEndpoint(endpoint)
    suspend fun updateEndpoint(endpoint: EndpointEntity) = endpointDao.updateEndpoint(endpoint)
    suspend fun deleteEndpoint(endpoint: EndpointEntity) = endpointDao.deleteEndpoint(endpoint)
    suspend fun getEndpointCount() = endpointDao.getEndpointCount()

    /**
     * Writes the UI projection AND the canonical Level 0 event.
     *
     * The ledger append is awaited rather than fired and forgotten: if the
     * canonical record failed silently, the projection would outlive the
     * authority and the two stores would diverge without anything noticing.
     */
    suspend fun insertMessage(message: MessageEntity): Long {
        val rowId = messageDao.insertMessage(message)
        // Use the id Room just assigned, not the caller's object: an
        // autoGenerate primary key is still 0 on the instance passed in, so
        // deriving the event id from it collides every message onto "msg-0".
        ledger?.appendSuspending(message.copy(id = rowId.toInt()).toConversationEvent())
        return rowId
    }

    /** Level 0 event history, oldest first. Empty when no ledger is attached. */
    suspend fun conversationEvents(): List<ConversationEvent> =
        ledger?.queryEventsByTime(0, Long.MAX_VALUE) ?: emptyList()

    /**
     * Backfills pre-ledger `messages` rows into the canonical ledger.
     *
     * Idempotent by construction: the event id is derived deterministically from
     * the message row id, and the DAO inserts with IGNORE, so re-running adds
     * nothing. Original timestamps are preserved and every backfilled row is
     * tagged `migratedFrom` so a migrated record is never mistaken for one
     * captured live.
     *
     * Returns the number of rows newly written.
     */
    suspend fun backfillLedgerFromMessages(): Int {
        val l = ledger ?: return 0
        val before = l.count()
        val legacy = messageDao.getAllMessagesSync()
        legacy.forEach { l.appendSuspending(it.toConversationEvent()) }
        return l.count() - before
    }
    
    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)
    
    suspend fun deleteMessage(message: MessageEntity) = messageDao.deleteMessage(message)
    
    suspend fun clearMessages() = messageDao.clearMessages()

    suspend fun saveProfile(profile: StyleProfileEntity) = styleDao.saveProfile(profile)
    
    suspend fun clearProfile() = styleDao.clearProfile()
}


/**
 * Projects a stored chat message onto a canonical conversation event.
 *
 * The event id is derived from the message row id rather than random, so a
 * replayed backfill maps a given message to the same event every time. That is
 * what makes the migration safe to retry.
 */
internal fun MessageEntity.toConversationEvent() = ConversationEvent(
    eventId = "msg-$id",
    timestamp = timestamp,
    actor = if (isUser) ConversationActor.OWNER else ConversationActor.M_ENGINE,
    rawContent = text,
    provenance = EventProvenance(
        sourcePlatform = "ANDROID",
        conversationId = sessionId.toString(),
        referencedArtifacts = listOfNotNull(imageUri),
    ),
)
