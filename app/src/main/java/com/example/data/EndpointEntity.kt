package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "endpoints")
data class EndpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val apiKey: String,
    val modelName: String,
    val type: String, // "OLLAMA" or "OPENAI"
    val isActive: Boolean = true,
    val isPrimary: Boolean = false
)
