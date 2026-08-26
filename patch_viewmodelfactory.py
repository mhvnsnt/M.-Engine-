import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target1 = """    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine,"""
new1 = """    private val memoryDao: MemoryFragmentDao,
    private val graphDao: com.example.data.GraphNodeDao,
    private val embeddingEngine: EmbeddingEngine,"""
content = content.replace(target1, new1)

target2 = "return ChatViewModel(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T"
new2 = "return ChatViewModel(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, graphDao, embeddingEngine, ttsEngine, context) as T"
content = content.replace(target2, new2)

target3 = "class ChatViewModel("
new3 = "class ChatViewModel("
# wait, ChatViewModel parameters need to be updated too!
target4 = """    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine,"""
new4 = """    private val memoryDao: MemoryFragmentDao,
    private val graphDao: com.example.data.GraphNodeDao,
    private val embeddingEngine: EmbeddingEngine,"""
content = content.replace(target4, new4)

target5 = "com.example.ai.ReflectionEngine(memoryDao, repository.graphDao, embeddingEngine, locationRepository)"
new5 = "com.example.ai.ReflectionEngine(memoryDao, graphDao, embeddingEngine, locationRepository)"
content = content.replace(target5, new5)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
