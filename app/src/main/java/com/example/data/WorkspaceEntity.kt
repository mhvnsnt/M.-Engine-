package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val githubRepoUrl: String = "",
    val branch: String = "main",
    val lastSyncTime: Long = 0
)

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val filePath: String,
    val content: String,
    val language: String = ""
)
