with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'r') as f:
    content = f.read()

content = content.replace('suspend fun checkHealth()', 'override suspend fun checkHealth()')
content = content.replace('suspend fun probeCapabilities()', 'override suspend fun probeCapabilities()')

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/ExecutionEnvironment.kt', 'r') as f:
    exec_content = f.read()

if 'suspend fun checkHealth()' not in exec_content:
    exec_content = exec_content.replace(
        'suspend fun probeCapabilities(): EnvironmentCapabilities',
        'suspend fun probeCapabilities(): EnvironmentCapabilities\n    suspend fun checkHealth(): Boolean = true'
    )
    with open('app/src/main/java/com/example/ai/capabilities/federated/environment/ExecutionEnvironment.kt', 'w') as f:
        f.write(exec_content)
