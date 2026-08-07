package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryFragmentDao {
    @Insert
    suspend fun insert(fragment: MemoryFragment)
    
    @Query("SELECT * FROM memory_fragments WHERE type = :type")
    suspend fun getFragmentsByType(type: String): List<MemoryFragment>

    @Query("SELECT * FROM memory_fragments")
    suspend fun getAllFragments(): List<MemoryFragment>

    @Query("DELETE FROM memory_fragments WHERE type = :type")
    suspend fun deleteFragmentsByType(type: String)

    @Query("DELETE FROM memory_fragments")
    suspend fun deleteAllFragments()
}