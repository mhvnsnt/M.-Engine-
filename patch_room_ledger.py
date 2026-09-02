import re

file_path = '/app/applet/app/src/main/java/com/example/data/RoomConversationLedger.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Add import
content = content.replace("import kotlinx.coroutines.runBlocking", "import kotlinx.coroutines.runBlocking\nimport com.example.ai.capabilities.ecology.RemoteControlPlaneRepository")

# Add field to RoomConversationLedger
content = content.replace(
"""    private val dao: ConversationEventDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
)""",
"""    private val dao: ConversationEventDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val remoteSync: RemoteControlPlaneRepository = RemoteControlPlaneRepository()
)"""
)

# Add push to appendSuspending
append_logic = """
    suspend fun appendSuspending(event: ConversationEvent) {
        dao.append(event.toEntity())
        
        // Push to canonical sync API
        try {
            val payload = mapOf(
                "eventId" to event.eventId,
                "timestamp" to event.timestamp,
                "actor" to event.actor.name,
                "content" to event.rawContent,
                "source" to "ANDROID",
                "conversationId" to event.provenance.conversationId
            )
            remoteSync.syncConversationEvents(listOf(payload))
        } catch (e: Exception) {
            // Offline or failed
        }
    }

    suspend fun syncFromCanonical() {
        try {
            // Find latest timestamp
            val latest = dao.recentActive(1).firstOrNull()?.timestamp ?: 0L
            val res = remoteSync.getConversationEvents(latest)
            res.getOrNull()?.forEach { ev ->
                val entity = ConversationEventEntity(
                    eventId = ev["eventId"] as? String ?: return@forEach,
                    timestamp = (ev["timestamp"] as? Number)?.toLong() ?: 0L,
                    actor = ev["actor"] as? String ?: "SYSTEM",
                    rawContent = ev["content"] as? String ?: "",
                    sourcePlatform = ev["source"] as? String ?: "UNKNOWN",
                    conversationId = ev["conversationId"] as? String ?: "default",
                    referencedArtifacts = "",
                    supersededByEventId = null,
                    migratedFrom = null
                )
                dao.append(entity)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
"""

content = re.sub(r'suspend fun appendSuspending\(event: ConversationEvent\) \{[\s\S]*?\}', append_logic.strip(), content)

with open(file_path, 'w') as f:
    f.write(content)
