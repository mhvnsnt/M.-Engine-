import re

with open('app/src/main/java/com/example/ai/capabilities/CognitiveKernel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'DELEGATE,\n    EXECUTING,',
    'DELEGATE,\n    SANDBOX_CREATING,\n    REPOSITORY_LOADING,\n    WORKER_STARTING,\n    EXECUTING,'
)

with open('app/src/main/java/com/example/ai/capabilities/CognitiveKernel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ai/capabilities/CognitiveKernelImpl.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'CognitiveState.DELEGATE to setOf(CognitiveState.EXECUTING, CognitiveState.FAILED, CognitiveState.CANCELLING),',
    'CognitiveState.DELEGATE to setOf(CognitiveState.SANDBOX_CREATING, CognitiveState.FAILED, CognitiveState.CANCELLING),\n        CognitiveState.SANDBOX_CREATING to setOf(CognitiveState.REPOSITORY_LOADING, CognitiveState.FAILED, CognitiveState.CANCELLING),\n        CognitiveState.REPOSITORY_LOADING to setOf(CognitiveState.WORKER_STARTING, CognitiveState.FAILED, CognitiveState.CANCELLING),\n        CognitiveState.WORKER_STARTING to setOf(CognitiveState.EXECUTING, CognitiveState.FAILED, CognitiveState.CANCELLING),'
)

with open('app/src/main/java/com/example/ai/capabilities/CognitiveKernelImpl.kt', 'w') as f:
    f.write(content)
