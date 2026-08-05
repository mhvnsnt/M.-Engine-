package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long

    @Query("SELECT * FROM files WHERE workspaceId = :workspaceId")
    fun getFilesForWorkspace(workspaceId: Long): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)
    
    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileById(fileId: Long): FileEntity?
}
