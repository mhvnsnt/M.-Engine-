import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('    private val ttsEngine: com.example.ai.TTSEngine,\n    private val repository: ChatRepository', '    private val repository: ChatRepository')

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

