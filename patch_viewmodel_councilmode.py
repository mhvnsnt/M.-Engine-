import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """    fun updatePullMemoryOnStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePullMemoryOnStart(enabled)
        }
    }"""
new_target = """    fun updatePullMemoryOnStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePullMemoryOnStart(enabled)
        }
    }
    
    fun updateCouncilMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCouncilMode(enabled)
        }
    }"""
content = content.replace(target, new_target)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
