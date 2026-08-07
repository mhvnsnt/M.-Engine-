import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

# Add SessionEntity to entities array
content = re.sub(r'entities = \[(.*?)\]', r'entities = [\1, SessionEntity::class]', content)
content = re.sub(r'version = \d+', 'version = 3', content)

if 'abstract fun sessionDao(): SessionDao' not in content:
    content = content.replace('abstract fun messageDao(): MessageDao', 'abstract fun messageDao(): MessageDao\n    abstract fun sessionDao(): SessionDao')

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)

