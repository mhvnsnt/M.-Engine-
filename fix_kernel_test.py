import re

with open('app/src/test/java/com/example/ai/capabilities/CognitiveKernelTest.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'kernel.transitionTo(CognitiveState.DELEGATE)\n        }\n                kernel.transitionTo(CognitiveState.EXECUTING)',
    'kernel.transitionTo(CognitiveState.DELEGATE)\n        }\n                kernel.transitionTo(CognitiveState.SANDBOX_CREATING)\n        kernel.transitionTo(CognitiveState.REPOSITORY_LOADING)\n        kernel.transitionTo(CognitiveState.WORKER_STARTING)\n        kernel.transitionTo(CognitiveState.EXECUTING)'
)

content = content.replace(
    'kernel.transitionTo(CognitiveState.DELEGATE)\n                for',
    'kernel.transitionTo(CognitiveState.DELEGATE)\n        kernel.transitionTo(CognitiveState.SANDBOX_CREATING)\n        kernel.transitionTo(CognitiveState.REPOSITORY_LOADING)\n        kernel.transitionTo(CognitiveState.WORKER_STARTING)\n                for'
)

with open('app/src/test/java/com/example/ai/capabilities/CognitiveKernelTest.kt', 'w') as f:
    f.write(content)
