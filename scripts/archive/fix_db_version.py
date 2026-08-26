import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

# find @Database(entities = [MessageEntity::class], version = 1)
content = re.sub(r'version = \d+', 'version = 2', content)

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)
