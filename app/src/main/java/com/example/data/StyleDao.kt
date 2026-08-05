package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleDao {
    @Query("SELECT * FROM style_profile WHERE id = 1")
    fun getProfile(): Flow<StyleProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: StyleProfileEntity)

    @Query("DELETE FROM style_profile")
    suspend fun clearProfile()
}
