import re

with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'package com.example.ai.capabilities',
    'package com.example.ai.capabilities\n\nimport com.example.ai.PermissionLevel'
)
content = content.replace('CapabilityType.SYSTEM_NATIVE', 'CapabilityType.REMOTE_AGENT')
content = content.replace('CapabilityStatus.ACTIVE', 'CapabilityStatus.ONLINE')

with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'w') as f:
    f.write(content)
