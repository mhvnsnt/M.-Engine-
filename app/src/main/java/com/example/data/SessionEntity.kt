package com.example.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "New Conversation",
    val timestamp: Long = System.currentTimeMillis()
)
