import re

with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

# find entities array and append GraphNode::class
content = re.sub(r'entities = \[(.*?)\]', r'entities = [\1, GraphNode::class]', content)
# increment version
content = re.sub(r'version = \d+', 'version = 5', content)

# add abstract fun graphNodeDao()
if 'abstract fun graphNodeDao' not in content:
    content = content.replace('abstract fun astroDao(): AstroDao', 'abstract fun astroDao(): AstroDao\n    abstract fun graphNodeDao(): GraphNodeDao')

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)
