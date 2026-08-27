import re

with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val score = eval.effectivenessScore * eval.efficiencyScore - eval.errorRate',
    'val score = eval.effectivenessScore * eval.efficiencyScore - eval.integrationComplexity'
)
content = content.replace(
    'var bestScore = -1f',
    'var bestScore = -1'
)

with open('app/src/main/java/com/example/ai/capabilities/WorkerPool.kt', 'w') as f:
    f.write(content)
