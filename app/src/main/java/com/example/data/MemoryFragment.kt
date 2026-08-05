package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_fragments")
data class MemoryFragment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var text: String = "",
    var timestamp: Long = 0,
    var isUser: Boolean = false,
    var embedding: String = "" // JSON representation of FloatArray
)
