import re
with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'r') as f:
    content = f.read()

content = content.replace('override override suspend fun checkHealth()', 'override suspend fun checkHealth()')
content = content.replace('override override suspend fun probeCapabilities()', 'override suspend fun probeCapabilities()')

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'w') as f:
    f.write(content)

def fix_secret(path):
    with open(path, 'r') as f:
        c = f.read()
    # RemoteFabricWorkerEnvironment(url = ...) -> add secret = ...
    # we know we replaced RemoteFabricWorkerEnvironment(worker.url) earlier, maybe we missed passing secret
    c = c.replace('RemoteFabricWorkerEnvironment(worker.url)', 'RemoteFabricWorkerEnvironment(worker.url, worker.secret)')
    with open(path, 'w') as f:
        f.write(c)

fix_secret('app/src/main/java/com/example/ai/capabilities/acquisition/CapabilityAcquisitionEngine.kt')
fix_secret('app/src/main/java/com/example/ai/capabilities/federated/environment/ExecutionPlacementEngine.kt')

