package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GraphNodeDao {
    @Insert
    suspend fun insert(node: GraphNode): Long

    @Update
    suspend fun update(node: GraphNode)

    @Query("SELECT * FROM knowledge_graph WHERE invalidAt IS NULL")
    suspend fun getActiveGraph(): List<GraphNode>
    
    @Query("SELECT * FROM knowledge_graph WHERE invalidAt IS NULL AND type = :type")
    suspend fun getActiveNodesByType(type: String): List<GraphNode>

    @Query("SELECT * FROM knowledge_graph WHERE subject = :subject AND invalidAt IS NULL")
    suspend fun getNodesBySubject(subject: String): List<GraphNode>
    
    @Query("UPDATE knowledge_graph SET invalidAt = :time WHERE id = :id")
    suspend fun invalidateNode(id: Long, time: Long = System.currentTimeMillis())
}
