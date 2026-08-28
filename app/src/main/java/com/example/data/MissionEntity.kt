package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ai.capabilities.MissionStatus

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goalDescription: String,
    val desiredOutcome: String,
    val currentState: MissionStatus,
    val historyJson: String,
    val subtasksJson: String
)
