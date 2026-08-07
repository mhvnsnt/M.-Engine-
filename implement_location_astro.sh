#!/bin/bash
set -e

# 1. Location & Region Entities
cat << 'KOTLIN' > app/src/main/java/com/example/data/LocationEntities.kt
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
KOTLIN

cat << 'KOTLIN' > app/src/main/java/com/example/data/LocationDao.kt
package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM user_constraints WHERE id = 1")
    fun getUserConstraintsFlow(): Flow<UserConstraints?>

    @Query("SELECT * FROM user_constraints WHERE id = 1")
    suspend fun getUserConstraints(): UserConstraints?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserConstraints(constraints: UserConstraints)

    @Insert
    suspend fun insertLocationSnapshot(snapshot: LocationSnapshot)

    @Query("SELECT * FROM region_profiles ORDER BY lastActiveTimestamp DESC LIMIT 1")
    suspend fun getMostRecentRegion(): RegionProfile?

    @Query("SELECT * FROM region_profiles WHERE regionId = :regionId")
    suspend fun getRegionProfile(regionId: String): RegionProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegionProfile(profile: RegionProfile)
    
    @Query("SELECT * FROM region_profiles ORDER BY lastActiveTimestamp DESC")
    fun getAllRegionsFlow(): Flow<List<RegionProfile>>
}
KOTLIN

# 2. Astro & Numerology Entities
cat << 'KOTLIN' > app/src/main/java/com/example/data/AstroEntities.kt
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
KOTLIN

cat << 'KOTLIN' > app/src/main/java/com/example/data/AstroDao.kt
package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AstroDao {
    @Query("SELECT * FROM astro_profiles WHERE id = 1")
    fun getAstroProfileFlow(): Flow<AstroProfile?>

    @Query("SELECT * FROM astro_profiles WHERE id = 1")
    suspend fun getAstroProfile(): AstroProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAstroProfile(profile: AstroProfile)
}
KOTLIN

# Update AppDatabase
sed -i 's/WorkspaceEntity::class, FileEntity::class]/WorkspaceEntity::class, FileEntity::class, UserConstraints::class, LocationSnapshot::class, RegionProfile::class, AstroProfile::class]/' app/src/main/java/com/example/data/AppDatabase.kt
sed -i 's/version = 2/version = 3/' app/src/main/java/com/example/data/AppDatabase.kt
sed -i '/abstract fun workspaceDao(): WorkspaceDao/a \    abstract fun locationDao(): LocationDao\n    abstract fun astroDao(): AstroDao' app/src/main/java/com/example/data/AppDatabase.kt

