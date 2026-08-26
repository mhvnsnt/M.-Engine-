import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'repository = ChatRepository(database.messageDao(), database.styleDao(), database.endpointDao())',
    'repository = ChatRepository(database.messageDao(), database.styleDao(), database.endpointDao(), database.sessionDao())'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

