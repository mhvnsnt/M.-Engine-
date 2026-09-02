import re

file_path = '/app/applet/app/src/main/java/com/example/ai/capabilities/ecology/RemoteControlPlaneRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Add endpoints to ControlPlaneApi interface
api_methods = """
    @POST("/api/v1/ledger/sync")
    suspend fun syncConversationEvents(@Body events: List<Map<String, Any>>): Map<String, Any>

    @GET("/api/v1/ledger/events")
    suspend fun getConversationEvents(@Query("since") since: Long): List<Map<String, Any>>
"""

content = content.replace("    @POST(\"/api/v1/control_plane/emergency_stop\")", api_methods + "\n    @POST(\"/api/v1/control_plane/emergency_stop\")")

# Add methods to RemoteControlPlaneRepository class
repo_methods = """
    suspend fun syncConversationEvents(events: List<Map<String, Any>>): Result<Map<String, Any>> {
        val cp = getApi() ?: return Result.failure(Exception("Not connected"))
        return try {
            Result.success(cp.syncConversationEvents(events))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationEvents(since: Long): Result<List<Map<String, Any>>> {
        val cp = getApi() ?: return Result.failure(Exception("Not connected"))
        return try {
            Result.success(cp.getConversationEvents(since))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
"""

content = content.replace("    suspend fun pause(): Result<Unit> {", repo_methods + "\n    suspend fun pause(): Result<Unit> {")

with open(file_path, 'w') as f:
    f.write(content)
