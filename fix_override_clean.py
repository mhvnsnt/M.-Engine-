with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'(override\s*)+', 'override ', content)

with open('app/src/main/java/com/example/ai/capabilities/federated/environment/RemoteFabricWorkerEnvironment.kt', 'w') as f:
    f.write(content)
