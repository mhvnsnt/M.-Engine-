import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

if "val autoSyncGithub" not in content:
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

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)

