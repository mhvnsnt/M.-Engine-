import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target1 = "    val pullMemoryOnStart: StateFlow<Boolean> = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)"
new1 = """    val pullMemoryOnStart: StateFlow<Boolean> = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val telegramBotToken: StateFlow<String> = settingsRepository.telegramBotTokenFlow.stateIn(viewModelScope, SharingStarted.Lazily, "")
    
    fun updateTelegramBotToken(token: String) {
        viewModelScope.launch {
            settingsRepository.updateTelegramBotToken(token)
        }
    }"""
content = content.replace(target1, new1)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
