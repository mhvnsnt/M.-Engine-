import re

def insert_methods(file_path, methods):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Strip the last '}'
    content = content.rstrip()
    if content.endswith('}'):
        content = content[:-1]
    
    with open(file_path, 'w') as f:
        f.write(content + "\n" + methods + "\n}\n")

sync_methods = """
    override fun syncConversationEvents(events: List<Map<String, Any>>): Map<String, Any> {
        var inserted = 0
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS conversation_events (event_id TEXT PRIMARY KEY, timestamp INTEGER, actor TEXT, content TEXT, source TEXT, conversation_id TEXT)")
            }
            conn.prepareStatement("INSERT OR IGNORE INTO conversation_events (event_id, timestamp, actor, content, source, conversation_id) VALUES (?, ?, ?, ?, ?, ?)").use { stmt ->
                for (event in events) {
                    stmt.setString(1, event["eventId"] as? String ?: "")
                    stmt.setLong(2, (event["timestamp"] as? Number)?.toLong() ?: 0L)
                    stmt.setString(3, event["actor"] as? String ?: "")
                    stmt.setString(4, event["content"] as? String ?: "")
                    stmt.setString(5, event["source"] as? String ?: "")
                    stmt.setString(6, event["conversationId"] as? String ?: "")
                    inserted += stmt.executeUpdate()
                }
            }
        }
        return mapOf("status" to "SYNCED", "inserted" to inserted)
    }

    override fun getConversationEvents(since: Long): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS conversation_events (event_id TEXT PRIMARY KEY, timestamp INTEGER, actor TEXT, content TEXT, source TEXT, conversation_id TEXT)")
                val rs = stmt.executeQuery("SELECT event_id, timestamp, actor, content, source, conversation_id FROM conversation_events WHERE timestamp > $since ORDER BY timestamp ASC")
                while (rs.next()) {
                    results.add(mapOf(
                        "eventId" to rs.getString("event_id"),
                        "timestamp" to rs.getLong("timestamp"),
                        "actor" to rs.getString("actor"),
                        "content" to rs.getString("content"),
                        "source" to rs.getString("source"),
                        "conversationId" to rs.getString("conversation_id")
                    ))
                }
            }
        }
        return results
    }
"""

insert_methods('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/SQLiteLedgerRepository.kt', sync_methods)
insert_methods('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/PostgresLedgerRepository.kt', sync_methods.replace("INSERT OR IGNORE", "INSERT INTO conversation_events (event_id, timestamp, actor, content, source, conversation_id) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING"))

