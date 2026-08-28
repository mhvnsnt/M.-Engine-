package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Insert
    suspend fun insertJob(job: JobEntity): Long

    @Update
    suspend fun updateJob(job: JobEntity)

    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJob(id: Long): JobEntity?
    
    @Query("SELECT * FROM jobs WHERE id = :id")
    fun getJobFlow(id: Long): Flow<JobEntity?>
}
