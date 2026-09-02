import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/ControlPlaneServer.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Add sync routes
sync_routes = """
        post("/api/v1/ledger/sync") {
            try {
                val payload = call.receive<List<Map<String, Any>>>()
                val result = ledger.syncConversationEvents(payload)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid payload")))
            }
        }
        
        get("/api/v1/ledger/events") {
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val events = ledger.getConversationEvents(since)
            call.respond(events)
        }
"""

# Insert before "get("/api/v1/mindstream")"
content = content.replace("get(\"/api/v1/mindstream\")", sync_routes + "\n        get(\"/api/v1/mindstream\")")

with open(file_path, 'w') as f:
    f.write(content)
