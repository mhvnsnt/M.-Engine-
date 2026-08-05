package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long
    
    @androidx.room.Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages")
    suspend fun clearMessages()
}
