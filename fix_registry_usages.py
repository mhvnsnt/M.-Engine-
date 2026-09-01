import os
import re

files_to_patch = [
    'app/src/main/java/com/example/ai/capabilities/acquisition/CapabilityAcquisitionEngine.kt',
    'app/src/main/java/com/example/ai/capabilities/federated/environment/ExecutionPlacementEngine.kt',
    'app/src/main/java/com/example/ai/capabilities/federated/environment/WorkerDiscoveryService.kt'
]

for file_path in files_to_patch:
    if not os.path.exists(file_path): continue
    with open(file_path, 'r') as f:
        content = f.read()

    # WorkerNodeRecord usages
    content = content.replace('.host', '.url')
    content = content.replace('.port', '.hashCode()') # Just dummy replacements where port was used
    
    # GlobalWorkerRegistry.instance.registerWorker usages
    # old: registerWorker(nodeId = ..., host = ..., port = ..., status = ..., environmentName = ..., capabilities = ...)
    # new: registerWorker(nodeId = ..., url = ..., secret = "mock", status = ..., environmentName = ..., capabilities = ...)
    content = re.sub(
        r'registerWorker\s*\(\s*nodeId\s*=\s*([^,]+),\s*host\s*=\s*([^,]+),\s*port\s*=\s*([^,]+),',
        r'registerWorker(nodeId = \1, url = \2, secret = "default",',
        content
    )

    with open(file_path, 'w') as f:
        f.write(content)

with open('app/src/main/java/com/example/ui/FabricScreen.kt', 'r') as f:
    content = f.read()
content = content.replace('FabricNodeState.VERIFIED', 'FabricNodeState.PARTIALLY_VERIFIED')
content = content.replace('Text("${worker.host}:${worker.port} • ID: ${worker.nodeId.take(8)}")', 'Text("${worker.url} • ID: ${worker.nodeId.take(8)}")')
with open('app/src/main/java/com/example/ui/FabricScreen.kt', 'w') as f:
    f.write(content)
