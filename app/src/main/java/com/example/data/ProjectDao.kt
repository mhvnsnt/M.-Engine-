package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY updatedAt DESC")
    suspend fun activeProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    /** Archive rather than delete: deleting would orphan Level 0 events. */
    @Query("UPDATE projects SET status = 'ARCHIVED', updatedAt = :at WHERE id = :id")
    suspend fun archiveProject(id: String, at: Long = System.currentTimeMillis())

    // --- associations ------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun associate(link: ProjectAssociationEntity)

    @Query("SELECT * FROM project_associations WHERE projectId = :projectId AND kind = :kind")
    suspend fun associationsOfKind(projectId: String, kind: String): List<ProjectAssociationEntity>

    @Query("SELECT * FROM project_associations WHERE projectId = :projectId")
    suspend fun allAssociations(projectId: String): List<ProjectAssociationEntity>

    /** Which project owns a given conversation/repo/job. */
    @Query("SELECT * FROM project_associations WHERE kind = :kind AND refId = :refId LIMIT 1")
    suspend fun findOwningProject(kind: String, refId: String): ProjectAssociationEntity?

    // --- project memory (Level 2) -----------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(entry: ProjectMemoryEntity)

    @Query("SELECT * FROM project_memory WHERE projectId = :projectId AND supersededBy IS NULL ORDER BY createdAt DESC")
    suspend fun activeMemory(projectId: String): List<ProjectMemoryEntity>

    @Query("SELECT * FROM project_memory WHERE projectId = :projectId ORDER BY createdAt ASC")
    suspend fun allMemory(projectId: String): List<ProjectMemoryEntity>

    @Query("UPDATE project_memory SET supersededBy = :successorId WHERE id = :id")
    suspend fun supersedeMemory(id: String, successorId: String)

    // --- artifacts (Library) ----------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtifact(artifact: ArtifactEntity)

    @Query("SELECT * FROM artifacts WHERE id = :id")
    suspend fun getArtifact(id: String): ArtifactEntity?

    /** Same bytes = same artifact, regardless of where they were written. */
    @Query("SELECT * FROM artifacts WHERE contentHash = :hash")
    suspend fun artifactsByHash(hash: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts WHERE projectId = :projectId AND supersededBy IS NULL ORDER BY createdAt DESC")
    suspend fun artifactsForProject(projectId: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    suspend fun artifactsForConversation(conversationId: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts WHERE jobId = :jobId ORDER BY createdAt DESC")
    suspend fun artifactsForJob(jobId: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentArtifacts(limit: Int): List<ArtifactEntity>

    @Query("UPDATE artifacts SET supersededBy = :successorId WHERE id = :id")
    suspend fun supersedeArtifact(id: String, successorId: String)

    // --- worker jobs -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: WorkerJobEntity)

    @Query("SELECT * FROM worker_jobs WHERE id = :id")
    suspend fun getJob(id: String): WorkerJobEntity?

    @Query("SELECT * FROM worker_jobs WHERE projectId = :projectId ORDER BY startedAt DESC")
    suspend fun jobsForProject(projectId: String): List<WorkerJobEntity>

    @Query("SELECT * FROM worker_jobs ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentJobs(limit: Int): Flow<List<WorkerJobEntity>>
}
