package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "style_profile")
data class StyleProfileEntity(
    @PrimaryKey val id: Int = 1,
    val totalMessages: Int = 0,
    val totalWords: Int = 0,
    val topics: String = "",
    val vocabulary: String = ""
)
