with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# 1. Add MemoryManager import
content = content.replace("import com.example.data.MemoryFragmentDao", "import com.example.data.MemoryFragmentDao\nimport com.example.github.MemoryManager")

# 2. GitHub PAT properties in Settings
content = content.replace('val githubPat: StateFlow<String> = settingsRepository.githubPatFlow.stateIn(', 'val autoSyncGithub: StateFlow<Boolean> = settingsRepository.autoSyncGithubFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)\n    val pullMemoryOnStart: StateFlow<Boolean> = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)\n\n    val githubPat: StateFlow<String> = settingsRepository.githubPatFlow.stateIn(')

methods = """    fun updateAutoSyncGithub(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSyncGithub(value)
        }
    }
    fun updatePullMemoryOnStart(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePullMemoryOnStart(value)
        }
    }
"""
content = content.replace("fun updateGithubPat(pat: String) {", methods + "\n    fun updateGithubPat(pat: String) {")

# 3. Add memory manager property inside ChatViewModel
content = content.replace(") : ViewModel() {\n", ") : ViewModel() {\n\n    val memoryManager = MemoryManager(context)\n", 1)

# 4. Add to Init block
init_block = """    init {
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
        }
"""
content = content.replace("    init {", init_block, 1)

# 5. Add syncMemory before ChatViewModelFactory
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
}
class ChatViewModelFactory"""
content = content.replace("}\nclass ChatViewModelFactory", sync_memory)

# 6. Replace _isGenerating.value = false with syncMemory() carefully
lines = content.split('\n')
new_lines = []
for line in lines:
    if "_isGenerating.value = false" in line and "private fun syncMemory()" not in "\n".join(new_lines[-10:]):
        new_lines.append(line.replace("_isGenerating.value = false", "syncMemory()"))
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write("\n".join(new_lines))
