package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)
    
    @Update
    suspend fun updateMission(mission: MissionEntity)
    
    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMission(id: String): MissionEntity?
    
    @Query("SELECT * FROM missions")
    suspend fun getAllMissions(): List<MissionEntity>
}
