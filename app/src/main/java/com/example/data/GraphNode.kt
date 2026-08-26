package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_graph")
data class GraphNode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val predicate: String,
    val obj: String,
    val validAt: Long = System.currentTimeMillis(),
    val invalidAt: Long? = null,
    val embedding: String = "", 
    val type: String = "EPISODIC" 
)
