import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target1 = "    private val treeSitterEngine = TreeSitterEngine(context)"
new1 = """    private val treeSitterEngine = TreeSitterEngine(context)
    private val lindyEngine = com.example.ai.LindyEngine(
        settingsRepository.telegramBotTokenFlow,
        codeJarvis,
        settingsRepository.githubPatFlow
    )"""
content = content.replace(target1, new1)

target2 = "        reflectionEngine.startReflectionLoop()"
new2 = """        reflectionEngine.startReflectionLoop()
        lindyEngine.startProactiveLoop { getPrimaryEndpointSync() }"""
content = content.replace(target2, new2)

target3 = "    }"
new3 = """    }
    
    private suspend fun getPrimaryEndpointSync(): com.example.data.EndpointEntity? {
        val active = repository.getActiveEndpoints()
        return active.find { it.isPrimary } ?: active.firstOrNull()
    }
"""

# Append the method to the class
# Replace the last closing brace with the method and the closing brace
content = re.sub(r'}\s*$', new3 + "}", content)


with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
