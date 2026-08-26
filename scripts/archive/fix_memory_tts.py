import re

with open('app/src/main/java/com/example/data/MemoryFragment.kt', 'r') as f:
    content = f.read()

old_memory = """@Entity(tableName = "memory_fragments")
data class MemoryFragment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var text: String = "",
    var timestamp: Long = 0,
    var isUser: Boolean = false,
    var embedding: String = "" // JSON representation of FloatArray
)"""

new_memory = """@Entity(tableName = "memory_fragments")
data class MemoryFragment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var text: String = "",
    var timestamp: Long = 0,
    var isUser: Boolean = false,
    var embedding: String = "", // JSON representation of FloatArray
    var type: String = "ARCHIVAL" // CORE, ARCHIVAL, WORKSPACE
)"""

content = content.replace(old_memory, new_memory)
with open('app/src/main/java/com/example/data/MemoryFragment.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/data/MemoryFragmentDao.kt', 'r') as f:
    content = f.read()

new_dao = """package com.example.data

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
}"""
with open('app/src/main/java/com/example/data/MemoryFragmentDao.kt', 'w') as f:
    f.write(new_dao)
