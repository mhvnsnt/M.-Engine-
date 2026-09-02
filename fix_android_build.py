import re

# Fix RemoteControlPlaneRepository.kt
file_path = '/app/applet/app/src/main/java/com/example/ai/capabilities/ecology/RemoteControlPlaneRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

if "import retrofit2.http.Query" not in content:
    content = content.replace("import retrofit2.http.POST", "import retrofit2.http.POST\nimport retrofit2.http.Query")

with open(file_path, 'w') as f:
    f.write(content)


# Fix RoomConversationLedger.kt
file_path2 = '/app/applet/app/src/main/java/com/example/data/RoomConversationLedger.kt'
with open(file_path2, 'r') as f:
    content2 = f.read()

# Instead of inline forEach, use a standard for loop
loop_fix = """
            val list = res.getOrNull() ?: emptyList()
            for (ev in list) {
                val entity = ConversationEventEntity(
                    eventId = ev["eventId"] as? String ?: continue,
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
"""

content2 = re.sub(r'res\.getOrNull\(\)\?\.forEach \{ ev ->[\s\S]*?\}', loop_fix.strip(), content2)

with open(file_path2, 'w') as f:
    f.write(content2)

