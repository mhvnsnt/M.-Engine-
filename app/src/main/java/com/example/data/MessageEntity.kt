package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val responderName: String? = null,
    val groupId: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: Long = 1L,
    val imageUri: String? = null
)
