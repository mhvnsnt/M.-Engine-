package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CapabilityKnowledgeDao {
    @Query("SELECT * FROM capability_knowledge WHERE capabilityName = :capabilityName")
    suspend fun getKnowledge(capabilityName: String): CapabilityKnowledgeEntity?

    @Query("SELECT * FROM capability_knowledge")
    suspend fun getAllKnowledge(): List<CapabilityKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(entity: CapabilityKnowledgeEntity)
}
