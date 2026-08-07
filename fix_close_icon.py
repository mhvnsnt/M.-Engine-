import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Icons.Filled.AccountCircle, contentDescription = "Remove"', 'androidx.compose.material.icons.Icons.Filled.Close, contentDescription = "Remove"')

if 'import androidx.compose.material.icons.filled.Close' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.AccountCircle', 'import androidx.compose.material.icons.filled.AccountCircle\nimport androidx.compose.material.icons.filled.Close')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
