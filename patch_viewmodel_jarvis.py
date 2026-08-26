import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = "val codeJarvis = CodeJarvis(codingTools, com.example.ai.TreeSitterEngine())"
new = "val codeJarvis = CodeJarvis(codingTools, com.example.ai.TreeSitterEngine(), graphDao)"
content = content.replace(target, new)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
