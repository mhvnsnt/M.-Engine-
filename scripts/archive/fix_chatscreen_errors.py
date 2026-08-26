import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Fix @Composable @Composable
content = content.replace('@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\n@Composable\n@Composable', '@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\n@Composable')

# Fix conflicting context
# Let's just find the local val context = LocalContext.current occurrences
contexts = re.findall(r'val context = LocalContext.current', content)
if len(contexts) > 1:
    # Keep the first, remove the others
    first_idx = content.find('val context = LocalContext.current')
    content = content[:first_idx + 1] + content[first_idx + 1:].replace('val context = LocalContext.current\n', '')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

