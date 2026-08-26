import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """    init {
        // Load system prompt from github if needed
        viewModelScope.launch {
            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
                val prompt = memoryManager.getSystemPromptLocal()
                if (!prompt.isNullOrBlank()) {
                    settingsRepository.updateSystemInstruction(prompt)
                }
            }
        }"""
        
replacement = """    init {
        // Load system prompt from github if needed
        viewModelScope.launch {
            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
            }
            val prompt = memoryManager.getSystemPromptLocal()
            if (!prompt.isNullOrBlank() && systemInstruction.value == com.example.data.SettingsRepository.DEFAULT_SYSTEM_INSTRUCTION) {
                settingsRepository.updateSystemInstruction(prompt)
            } else if (!prompt.isNullOrBlank() && pullMemoryOnStart.value) {
                settingsRepository.updateSystemInstruction(prompt)
            }
        }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)

