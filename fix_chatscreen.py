import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Replace Text(text = message.text) with MarkdownText
content = content.replace(
    'Text(text = message.text, color = contentColor)',
    'MarkdownText(text = message.text, modifier = Modifier)'
)

# Fix imports if needed
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

