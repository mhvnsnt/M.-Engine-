package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "astro_profiles")
data class AstroProfile(
    @PrimaryKey val id: Int = 1,
    val birthDateStr: String = "", // e.g., "1990-01-01"
    val birthTimeStr: String = "", // e.g., "12:00"
    val birthLocation: String = "",
    val lifePathNumber: Int = 0,
    val expressionNumber: Int = 0,
    val soulUrgeNumber: Int = 0,
    val placementsJson: String = "{}"
)
