import re

interface_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/AgencyLedgerRepository.kt'
with open(interface_path, 'r') as f:
    interface_content = f.read()

# Extract all method signatures
methods = re.findall(r'fun\s+(\w+)\s*\((.*?)\)(?:\s*:\s*(.*?))?$', interface_content, re.MULTILINE)

imports = """package com.example.ai.cloud
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SQLiteLedgerRepository(private val dbPath: String = "jdbc:sqlite:agency_ledger.db") : AgencyLedgerRepository {
    private val capabilitiesStore = ConcurrentHashMap<String, MutableMap<String, Any>>()
    private val activeWorkerJobs = ConcurrentHashMap<String, MutableMap<String, Any>>()
    private var activeCycleState: MutableMap<String, Any>? = null
    private val tandemSignals = mutableListOf<Map<String, Any>>()
    private val causalRecords = mutableListOf<Map<String, Any>>()
    private val pendingJobs = mutableListOf<Map<String, Any>>(
        mapOf("jobId" to "TEST-JOB-001", "operation" to "TEST_ARTIFACT", "params" to emptyMap<String, Any>())
    )

    private fun getConnection(): Connection {
        return DriverManager.getConnection(dbPath)
    }

"""

body = imports

for name, args_str, ret_type in methods:
    ret_type = ret_type.strip()
    
    # Custom implementations for important ones
    if name == "syncConversationEvents":
        impl = """        var inserted = 0
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
        return mapOf("status" to "SYNCED", "inserted" to inserted)"""
    elif name == "getConversationEvents":
        impl = """        val results = mutableListOf<Map<String, Any>>()
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
        return results"""
    elif name == "enrollWorker":
        impl = """        return mapOf("status" to "ENROLLED", "workerId" to workerId)"""
    elif name == "heartbeatWorker":
        impl = """        return mapOf("status" to "OK")"""
    elif name == "leaseJob":
        impl = """        if (pendingJobs.isNotEmpty()) {
            return pendingJobs.removeAt(0)
        }
        return null"""
    elif name == "createJob":
        impl = """        val jobId = "job-${System.currentTimeMillis()}"
        val job = mapOf("jobId" to jobId, "operation" to operation, "status" to "CREATED")
        pendingJobs.add(job)
        return job"""
    elif name == "completeJob":
        impl = """        return true"""
    elif name == "registerArtifact":
        impl = """        return mapOf("artifactId" to "art-${System.currentTimeMillis()}", "uri" to uri)"""
    else:
        # Default stubs
        if not ret_type or ret_type == "Unit":
            impl = "        // stub"
        elif ret_type == "Boolean":
            impl = "        return false"
        elif "List" in ret_type:
            impl = "        return emptyList()"
        elif "Map" in ret_type:
            if "?" in ret_type:
                impl = "        return null"
            else:
                impl = "        return emptyMap()"
        elif ret_type == "String?":
            impl = "        return null"
        else:
            impl = "        return null!! // fallback stub"
            
    body += f"    override fun {name}({args_str}){': ' + ret_type if ret_type else ''} {{\n{impl}\n    }}\n\n"

body += "}\n"

with open('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/SQLiteLedgerRepository.kt', 'w') as f:
    f.write(body)

# Same for Postgres but just switch the INSERT statement
body_pg = body.replace("class SQLiteLedgerRepository", "class PostgresLedgerRepository")
body_pg = body_pg.replace("INSERT OR IGNORE INTO conversation_events", "INSERT INTO conversation_events (event_id, timestamp, actor, content, source, conversation_id) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING")
# also need to remove the first VALUES (?, ?, ?, ?, ?, ?) that was duplicated in the replace above
body_pg = re.sub(r'VALUES \(\?, \?, \?, \?, \?, \?\) ON CONFLICT', 'ON CONFLICT', body_pg)

with open('/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/PostgresLedgerRepository.kt', 'w') as f:
    f.write(body_pg)

