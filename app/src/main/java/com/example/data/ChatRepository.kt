package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val messageDao: MessageDao,
    private val styleDao: StyleDao,
    private val endpointDao: EndpointDao,
    private val sessionDao: SessionDao
) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>> = messageDao.getMessagesForSession(sessionId)
    
    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)
    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)

    val styleProfile: Flow<StyleProfileEntity?> = styleDao.getProfile()
    val allEndpoints: Flow<List<EndpointEntity>> = endpointDao.getAllEndpoints()

    suspend fun getActiveEndpoints() = endpointDao.getActiveEndpoints()
    suspend fun getPrimaryEndpoint() = endpointDao.getPrimaryEndpoint()
    suspend fun insertEndpoint(endpoint: EndpointEntity) = endpointDao.insertEndpoint(endpoint)
    suspend fun updateEndpoint(endpoint: EndpointEntity) = endpointDao.updateEndpoint(endpoint)
    suspend fun deleteEndpoint(endpoint: EndpointEntity) = endpointDao.deleteEndpoint(endpoint)
    suspend fun getEndpointCount() = endpointDao.getEndpointCount()

    suspend fun insertMessage(message: MessageEntity): Long = messageDao.insertMessage(message)
    
    suspend fun updateMessage(message: MessageEntity) = messageDao.updateMessage(message)
    
    suspend fun clearMessages() = messageDao.clearMessages()

    suspend fun saveProfile(profile: StyleProfileEntity) = styleDao.saveProfile(profile)
    
    suspend fun clearProfile() = styleDao.clearProfile()
}
