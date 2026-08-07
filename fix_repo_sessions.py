import re

with open('app/src/main/java/com/example/data/ChatRepository.kt', 'r') as f:
    content = f.read()

if 'private val sessionDao: SessionDao' not in content:
    content = content.replace(
        'private val endpointDao: EndpointDao\n)',
        'private val endpointDao: EndpointDao,\n    private val sessionDao: SessionDao\n)'
    )
    
    # Add session methods
    methods = """
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>> = messageDao.getMessagesForSession(sessionId)
    
    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: SessionEntity) = sessionDao.updateSession(session)
    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)
"""
    content = content.replace('val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()', 'val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()' + methods)
    
    with open('app/src/main/java/com/example/data/ChatRepository.kt', 'w') as f:
        f.write(content)

