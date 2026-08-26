import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '@OptIn(ExperimentalMaterial3Api::class)\n@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)',
    '@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)'
)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
