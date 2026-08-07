import re

with open('app/src/main/java/com/example/data/MessageEntity.kt', 'r') as f:
    content = f.read()

if 'val sessionId: Long = 1L' not in content:
    content = content.replace(
        'val timestamp: Long = System.currentTimeMillis(),',
        'val timestamp: Long = System.currentTimeMillis(),\n    val sessionId: Long = 1L,'
    )
    with open('app/src/main/java/com/example/data/MessageEntity.kt', 'w') as f:
        f.write(content)

session_entity = """package com.example.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "New Conversation",
    val timestamp: Long = System.currentTimeMillis()
)
"""
with open('app/src/main/java/com/example/data/SessionEntity.kt', 'w') as f:
    f.write(session_entity)

session_dao = """package com.example.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Insert
    suspend fun insertSession(session: SessionEntity): Long
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
"""
with open('app/src/main/java/com/example/data/SessionDao.kt', 'w') as f:
    f.write(session_dao)

