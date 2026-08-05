package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryFragmentDao {
    @Insert
    suspend fun insert(fragment: MemoryFragment)

    @Query("SELECT * FROM memory_fragments")
    suspend fun getAllFragments(): List<MemoryFragment>
}
