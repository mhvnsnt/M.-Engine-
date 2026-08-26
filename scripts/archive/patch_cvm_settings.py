with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """    private val settingsRepository: SettingsRepository,"""
replacement = """    val settingsRepository: SettingsRepository,"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
