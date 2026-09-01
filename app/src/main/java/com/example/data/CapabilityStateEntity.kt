package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "capability_state")
data class CapabilityStateEntity(
    @PrimaryKey val capabilityId: String,
    val state: String,
    val circuitState: String,
    val lastHealthCheck: Long?,
    val verificationEvidence: String // JSON array of strings
)
