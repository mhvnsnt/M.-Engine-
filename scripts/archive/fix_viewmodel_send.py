import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('fun sendMessage(text: String) {', 'fun sendMessage(text: String, imageUri: String? = null) {')
content = content.replace('val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId)', 'val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri)')

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
