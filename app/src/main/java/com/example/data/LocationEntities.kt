package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_constraints")
data class UserConstraints(
    @PrimaryKey val id: Int = 1,
    val budgetMode: Boolean = true,
    val entryCostFilter: String = "ANY", // FREE_ONLY, LOW_COST, ANY
    val userRole: String = "ARTIST_NETWORKING", // ARTIST_NETWORKING, TOURIST, LOCAL
    val excludedCategories: String = "[]", // JSON list
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "location_snapshots")
data class LocationSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val geocodedAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: Long = 0
)

@Entity(tableName = "region_profiles")
data class RegionProfile(
    @PrimaryKey val regionId: String,
    val displayName: String,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val localNotes: String = "[]" // JSON list
)
