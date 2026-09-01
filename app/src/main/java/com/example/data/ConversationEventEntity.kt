package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical Level 0 record — the immutable conversation/operational event.
 *
 * Physical storage for `ImmutableConversationLedger`. It lives in the existing
 * Room database on purpose: that database is already opened through SQLCipher,
 * so conversation history stays encrypted at rest. The alternative
 * implementation in the memory package, `FileBackedConversationLedger`, writes
 * Java-serialized objects to a plain file — which would be both a second
 * authority and an unencrypted copy of everything the owner has ever said.
 *
 * Nothing in the application updates or deletes rows in this table. Corrections
 * are expressed by appending a new event and setting `supersededByEventId` on
 * the old one's successor chain — history is added to, never rewritten.
 */
@Entity(
    tableName = "conversation_events",
    indices = [Index("timestamp"), Index("conversationId"), Index("actor")],
)
data class ConversationEventEntity(
    @PrimaryKey val eventId: String,
    val timestamp: Long,
    val actor: String,
    val rawContent: String,
    val sourcePlatform: String,
    val conversationId: String,
    /** Comma-separated artifact references. Empty when none. */
    val referencedArtifacts: String = "",
    val supersededByEventId: String? = null,
    /**
     * Set only for rows created by backfilling pre-ledger Room messages, so a
     * migrated record is never mistaken for one that originated here.
     * Null for events captured live.
     */
    val migratedFrom: String? = null,
)
