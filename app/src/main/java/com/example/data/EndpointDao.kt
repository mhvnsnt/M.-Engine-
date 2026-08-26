package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EndpointDao {
    @Query("SELECT * FROM endpoints")
    fun getAllEndpoints(): Flow<List<EndpointEntity>>
    @Query("SELECT * FROM endpoints")
    suspend fun getAllEndpointsSync(): List<EndpointEntity>
    
    @Query("SELECT * FROM endpoints WHERE isActive = 1")
    suspend fun getActiveEndpoints(): List<EndpointEntity>
    
    @Query("SELECT * FROM endpoints WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryEndpoint(): EndpointEntity?

    @Query("UPDATE endpoints SET apiKey = :apiKey WHERE id = :id")
    suspend fun updateApiKey(id: Int, apiKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: EndpointEntity)

    @Update
    suspend fun updateEndpoint(endpoint: EndpointEntity)

    @Delete
    suspend fun deleteEndpoint(endpoint: EndpointEntity)
    
    @Query("SELECT COUNT(*) FROM endpoints")
    suspend fun getEndpointCount(): Int
}
