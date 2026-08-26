import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

sync_memory = """    private fun syncMemory() {
        _isGenerating.value = false
        viewModelScope.launch {
            val msgs = messages.value
            memoryManager.saveConversationLocal(System.currentTimeMillis(), msgs)
            if (autoSyncGithub.value) {
                memoryManager.syncSessionToGithub(githubPat.value, System.currentTimeMillis(), msgs)
            }
        }
    }
"""

content = content.replace("    fun clearLocationSnapshots() {", sync_memory + "\n    fun clearLocationSnapshots() {")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)

