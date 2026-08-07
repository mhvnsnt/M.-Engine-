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

    @Query("DELETE FROM region_profiles")
    suspend fun deleteAllRegions()

    @Query("DELETE FROM region_profiles WHERE regionId = :regionId")
    suspend fun deleteRegion(regionId: String)

    @Query("DELETE FROM location_snapshots")
    suspend fun deleteSnapshots()
}
