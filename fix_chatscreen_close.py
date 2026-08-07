import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('androidx.compose.material.icons.Icons.Filled.Close', 'Icons.Filled.Close')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
