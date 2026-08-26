import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Add the properties before init {
properties = """
    private val treeSitterEngine = com.example.ai.TreeSitterEngine(context)
    private val reflectionEngine = com.example.ai.ReflectionEngine(memoryDao, graphDao, embeddingEngine, locationRepository)
    private val lindyEngine = com.example.ai.LindyEngine(settingsRepository.telegramBotTokenFlow, codeJarvis, settingsRepository.githubPatFlow)
"""
content = content.replace("    init {", properties + "\n    init {")

# Add the start loops inside init {
loops = """
        reflectionEngine.startReflectionLoop()
        lindyEngine.startProactiveLoop { getPrimaryEndpointSync() }
"""
content = content.replace("    init {\n        // Load system prompt", "    init {\n" + loops + "        // Load system prompt")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
