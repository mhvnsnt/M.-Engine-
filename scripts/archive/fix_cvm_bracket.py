import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Let's inspect the end of the file.
sync_mem_str = """    private fun syncMemory() {
        _isGenerating.value = false
        viewModelScope.launch {
            val msgs = messages.value
            memoryManager.saveConversationLocal(System.currentTimeMillis(), msgs)
            if (autoSyncGithub.value) {
                memoryManager.syncSessionToGithub(githubPat.value, System.currentTimeMillis(), msgs)
            }
        }
    }
}
class ChatViewModelFactory"""

if "}\nclass ChatViewModelFactory" in content:
    print("Found exact closing bracket before ChatViewModelFactory")

