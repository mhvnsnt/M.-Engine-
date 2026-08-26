import re

with open('app/src/main/java/com/example/data/MessageEntity.kt', 'r') as f:
    content = f.read()

if 'imageUri: String?' not in content:
    content = content.replace(
        'val timestamp: Long = System.currentTimeMillis()',
        'val timestamp: Long = System.currentTimeMillis(),\n    val imageUri: String? = null'
    )
    with open('app/src/main/java/com/example/data/MessageEntity.kt', 'w') as f:
        f.write(content)
