with open('app/src/main/java/com/example/ai/capabilities/federated/environment/WorkerRegistry.kt', 'r') as f:
    content = f.read()

old_data = '''data class WorkerNodeRecord(
    val nodeId: String,
    val host: String,
    val port: Int,
    var status: FabricNodeState, // Changed from String to FabricNodeState
    val environmentName: String,
    val capabilities: EnvironmentCapabilities
)'''

new_data = '''data class WorkerNodeRecord(
    val nodeId: String,
    val url: String,
    val secret: String,
    var status: FabricNodeState,
    val environmentName: String,
    val capabilities: EnvironmentCapabilities
)'''

content = content.replace(old_data, new_data)

old_reg = '''fun registerWorker(nodeId: String, host: String, port: Int, status: FabricNodeState, environmentName: String, capabilities: EnvironmentCapabilities) {
        val newRecord = WorkerNodeRecord(nodeId, host, port, status, environmentName, capabilities)'''

new_reg = '''fun registerWorker(nodeId: String, url: String, secret: String, status: FabricNodeState, environmentName: String, capabilities: EnvironmentCapabilities) {
        val newRecord = WorkerNodeRecord(nodeId, url, secret, status, environmentName, capabilities)'''

content = content.replace(old_reg, new_reg)

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/WorkerRegistry.kt', 'w') as f:
    f.write(content)
