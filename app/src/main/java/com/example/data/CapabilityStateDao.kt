package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CapabilityStateDao {
    @Query("SELECT * FROM capability_state")
    suspend fun getAllStates(): List<CapabilityStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateState(entity: CapabilityStateEntity)
}
