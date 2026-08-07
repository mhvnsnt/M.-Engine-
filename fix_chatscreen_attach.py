import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

attach_button = """IconButton(
                    onClick = { /* TODO: Open file picker */ },
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("attach_button")
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
"""

if 'Icons.Filled.AttachFile' not in content:
    content = content.replace('Spacer(modifier = Modifier.width(8.dp))\n                IconButton(', 'Spacer(modifier = Modifier.width(8.dp))\n                ' + attach_button + '\n                IconButton(')
    content = content.replace('import androidx.compose.material.icons.filled.Mic', 'import androidx.compose.material.icons.filled.Mic\nimport androidx.compose.material.icons.filled.AttachFile')
    
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

