with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'var completeResponse = ""\n                var lastUpdateTime = System.currentTimeMillis()',
    'var completeResponse = ""\n                var ttsBuffer = ""\n                var lastUpdateTime = System.currentTimeMillis()'
)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
