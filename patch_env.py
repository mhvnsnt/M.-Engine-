with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'r') as f:
    content = f.read()

# Add secret to constructor
old_cons = '''class RemoteFabricWorkerEnvironment(
    private val workerUrl: String,
    override val environmentId: String = "env_remote_worker_${workerUrl.hashCode()}",
    initialCapabilities: EnvironmentCapabilities? = null,
    initialEnvironmentName: String? = null
) : ExecutionEnvironment {'''

new_cons = '''class RemoteFabricWorkerEnvironment(
    private val workerUrl: String,
    private val secret: String,
    override val environmentId: String = "env_remote_worker_${workerUrl.hashCode()}",
    initialCapabilities: EnvironmentCapabilities? = null,
    initialEnvironmentName: String? = null
) : ExecutionEnvironment {'''

content = content.replace(old_cons, new_cons)

# Add health check
health_check = '''
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$workerUrl/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $secret")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val success = connection.responseCode == 200
            if (success && nodeState != FabricNodeState.AVAILABLE) {
                nodeState = FabricNodeState.AVAILABLE
            }
            return@withContext success
        } catch (e: Exception) {
            nodeState = FabricNodeState.UNAVAILABLE
            return@withContext false
        }
    }
'''

content = content.replace('suspend fun probeCapabilities', health_check + '\n    suspend fun probeCapabilities')

# Add Authorization headers to probe
content = content.replace(
    'connection.requestMethod = "GET"\n            connection.connectTimeout = 5000',
    'connection.requestMethod = "GET"\n            connection.setRequestProperty("Authorization", "Bearer $secret")\n            connection.connectTimeout = 5000'
)

# Add Authorization headers to execute
content = content.replace(
    'connection.setRequestProperty("Content-Type", "application/json")',
    'connection.setRequestProperty("Content-Type", "application/json")\n            connection.setRequestProperty("Authorization", "Bearer $secret")'
)

# Add Authorization to polling
content = content.replace(
    'pollConn.requestMethod = "GET"',
    'pollConn.requestMethod = "GET"\n                    pollConn.setRequestProperty("Authorization", "Bearer $secret")'
)

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'w') as f:
    f.write(content)
