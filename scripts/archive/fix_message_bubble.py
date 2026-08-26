import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add Image to MessageBubble
old_bubble = """                Text(
                    text = message.text,
                    color = textColor
                )"""

new_bubble = """                if (message.imageUri != null) {
                    AsyncImage(
                        model = android.net.Uri.parse(message.imageUri),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
                MarkdownText(
                    text = message.text,
                    modifier = Modifier,
                    color = textColor
                )"""

if 'if (message.imageUri != null)' not in content:
    content = content.replace(old_bubble, new_bubble)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

