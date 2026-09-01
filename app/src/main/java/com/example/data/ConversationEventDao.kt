package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Append and read only.
 *
 * There is deliberately no @Update and no @Delete. Level 0 is immutable, and the
 * cheapest way to keep it that way is to give callers no verb that could rewrite
 * it. The one mutating query, [markSuperseded], records a correction by pointing
 * an event at its successor; the superseded event itself is preserved intact.
 */
@Dao
interface ConversationEventDao {

    /** IGNORE makes replaying a migration idempotent: a re-run adds nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun append(event: ConversationEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun appendAll(events: List<ConversationEventEntity>): List<Long>

    @Query("SELECT * FROM conversation_events WHERE eventId = :eventId")
    suspend fun getEvent(eventId: String): ConversationEventEntity?

    @Query("SELECT * FROM conversation_events ORDER BY timestamp ASC")
    suspend fun getAll(): List<ConversationEventEntity>

    @Query("SELECT * FROM conversation_events ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ConversationEventEntity>>

    @Query(
        "SELECT * FROM conversation_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC",
    )
    suspend fun queryByTime(start: Long, end: Long): List<ConversationEventEntity>

    @Query(
        "SELECT * FROM conversation_events WHERE conversationId = :conversationId ORDER BY timestamp ASC",
    )
    suspend fun getForConversation(conversationId: String): List<ConversationEventEntity>

    /** Active = not corrected by a later event. */
    @Query(
        "SELECT * FROM conversation_events WHERE supersededByEventId IS NULL ORDER BY timestamp DESC LIMIT :limit",
    )
    suspend fun recentActive(limit: Int): List<ConversationEventEntity>

    @Query("UPDATE conversation_events SET supersededByEventId = :successorId WHERE eventId = :eventId")
    suspend fun markSuperseded(eventId: String, successorId: String)

    @Query("SELECT COUNT(*) FROM conversation_events")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM conversation_events WHERE migratedFrom IS NOT NULL")
    suspend fun migratedCount(): Int
}
