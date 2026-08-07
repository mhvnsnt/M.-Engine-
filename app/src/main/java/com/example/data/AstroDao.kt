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
