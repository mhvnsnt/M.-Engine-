with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = "private val lindyEngine = com.example.ai.LindyEngine(settingsRepository.telegramBotTokenFlow, codeJarvis, settingsRepository.githubPatFlow)"
replacement = "private val lindyEngine = com.example.ai.LindyEngine(settingsRepository.telegramBotTokenFlow, codeJarvis, settingsRepository.githubPatFlow, codingTools)"

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
print("Patched ChatViewModel.kt successfully")
