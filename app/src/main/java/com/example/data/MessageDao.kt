package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    /** One-shot read, used by the ledger backfill. */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long
    
    @androidx.room.Update
    suspend fun updateMessage(message: MessageEntity)
    
    @androidx.room.Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages")
    suspend fun clearMessages()
}
