import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
            }"""
replacement = """            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
                val prompt = memoryManager.getSystemPromptLocal()
                if (!prompt.isNullOrBlank()) {
                    settingsRepository.updateSystemInstruction(prompt)
                }
            }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
