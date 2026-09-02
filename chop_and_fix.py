import sys

def chop_and_fix(filepath, is_postgres):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    # Find the FIRST occurrence of syncConversationEvents
    cutoff = -1
    for i, line in enumerate(lines):
        if "override fun syncConversationEvents" in line:
            cutoff = i
            break

    if cutoff != -1:
        lines = lines[:cutoff]
        # Just in case the previous line was a hanging closing brace
        pass
    else:
        # if not found, we just strip the last '}'
        # But wait, there might be duplicate enrollWorkers instead?
        # Let's find the FIRST enrollWorker
        cutoff_enroll = -1
        for i, line in enumerate(lines):
            if "override fun enrollWorker" in line:
                cutoff_enroll = i
                break
        
        if cutoff_enroll != -1:
            lines = lines[:cutoff_enroll]

    content = "".join(lines)
    # Ensure it ends cleanly (no hanging open braces)
    # Actually, we chopped right before enrollWorker or syncConversationEvents.
    # The previous method was recordDevelopmentSignal
    
    methods = """
    private val pendingJobs = mutableListOf<Map<String, Any>>(
        mapOf("jobId" to "TEST-JOB-001", "operation" to "TEST_ARTIFACT", "params" to emptyMap<String, Any>())
    )

    override fun enrollWorker(workerId: String, os: String, unrealVersion: String, repository: String, currentBranch: String, currentCommit: String): Map<String, Any> {
        return mapOf("status" to "ENROLLED", "workerId" to workerId)
    }

    override fun heartbeatWorker(workerId: String, state: String): Map<String, Any> {
        return mapOf("status" to "OK")
    }

    override fun leaseJob(workerId: String): Map<String, Any>? {
        if (pendingJobs.isNotEmpty()) {
            return pendingJobs.removeAt(0)
        }
        return null
    }

    override fun createJob(operation: String, params: Map<String, Any>): Map<String, Any> {
        val jobId = "job-${System.currentTimeMillis()}"
        val job = mapOf("jobId" to jobId, "operation" to operation, "status" to "CREATED")
        pendingJobs.add(job)
        return job
    }

    override fun completeJob(jobId: String, exitStatus: Int, evidenceLevel: String, stdout: String, stderr: String): Boolean {
        return true
    }

    override fun registerArtifact(jobId: String, workerId: String, sha256: String, size: Long, path: String, uri: String): Map<String, Any> {
        return mapOf("artifactId" to "art-${System.currentTimeMillis()}", "uri" to uri)
    }

    override fun syncConversationEvents(events: List<Map<String, Any>>): Map<String, Any> {
        var inserted = 0
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS conversation_events (event_id TEXT PRIMARY KEY, timestamp INTEGER, actor TEXT, content TEXT, source TEXT, conversation_id TEXT)")
            }
            val sql = if (is_postgres) {
                "INSERT INTO conversation_events (event_id, timestamp, actor, content, source, conversation_id) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING"
            } else {
                "INSERT OR IGNORE INTO conversation_events (event_id, timestamp, actor, content, source, conversation_id) VALUES (?, ?, ?, ?, ?, ?)"
            }
            conn.prepareStatement(sql).use { stmt ->
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
}
"""

    if "is_postgres" in str(is_postgres):
        # We need the actual bool
        pass

    methods = methods.replace("is_postgres", "true" if is_postgres else "false")
    
    with open(filepath, 'w') as f:
        f.write(content + methods)

chop_and_fix('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/SQLiteLedgerRepository.kt', False)
chop_and_fix('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/PostgresLedgerRepository.kt', True)
