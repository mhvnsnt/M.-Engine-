import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target1 = "    val openRouterKey: StateFlow<String> = settingsRepository.openRouterKeyFlow.stateIn(viewModelScope, SharingStarted.Lazily, \"\")"
new1 = """    val openRouterKey: StateFlow<String> = settingsRepository.openRouterKeyFlow.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val telegramBotToken: StateFlow<String> = settingsRepository.telegramBotTokenFlow.stateIn(viewModelScope, SharingStarted.Lazily, "")
    
    fun updateTelegramBotToken(token: String) {
        viewModelScope.launch {
            settingsRepository.updateTelegramBotToken(token)
        }
    }"""
content = content.replace(target1, new1)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
