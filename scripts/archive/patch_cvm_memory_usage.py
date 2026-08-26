import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

if "val memoryManager = com.example.github.MemoryManager" not in content:
    content = content.replace("private val context: android.content.Context", "private val context: android.content.Context\n) : ViewModel() {\n\n    val memoryManager = com.example.github.MemoryManager(context)")

init_block = """    init {
        // Load system prompt from github if needed
        viewModelScope.launch {
            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
            }
        }
"""
if "memoryManager.pullSystemPrompt" not in content:
    content = content.replace("    init {", init_block)

# Add save logic after generating response
save_logic = """
            // Save to memory manager
            viewModelScope.launch {
                val msgs = _messages.value
                memoryManager.saveConversationLocal(System.currentTimeMillis(), msgs)
                if (autoSyncGithub.value) {
                    memoryManager.syncSessionToGithub(githubPat.value, System.currentTimeMillis(), msgs)
                }
            }
"""
if "memoryManager.saveConversationLocal" not in content:
    content = content.replace("_isGenerating.value = false", "_isGenerating.value = false\n" + save_logic)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)

