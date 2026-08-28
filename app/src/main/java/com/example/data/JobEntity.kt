package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val status: String,
    val logs: String = "",
    val repository: String = "",
    val branch: String = "",
    val baseCommit: String = "",
    val currentCommit: String = "",
    val currentState: String = "",
    val currentCycle: Int = 0,
    val workerId: String = "",
    val lastCheckpointId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
