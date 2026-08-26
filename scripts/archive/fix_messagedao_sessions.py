import re

with open('app/src/main/java/com/example/data/MessageDao.kt', 'r') as f:
    content = f.read()

if 'fun getMessagesForSession' not in content:
    content = content.replace(
        'fun getAllMessages(): Flow<List<MessageEntity>>',
        'fun getAllMessages(): Flow<List<MessageEntity>>\n\n    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")\n    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>>'
    )
    with open('app/src/main/java/com/example/data/MessageDao.kt', 'w') as f:
        f.write(content)
