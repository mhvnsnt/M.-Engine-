import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

if 'import com.example.ai.TTSEngine' not in content:
    content = content.replace('import com.example.ai.EmbeddingEngine', 'import com.example.ai.EmbeddingEngine\nimport com.example.ai.TTSEngine')
    
    content = content.replace('private lateinit var embeddingEngine: EmbeddingEngine', 'private lateinit var embeddingEngine: EmbeddingEngine\n    private lateinit var ttsEngine: TTSEngine')
    
    content = content.replace('embeddingEngine = EmbeddingEngine(applicationContext)', 'embeddingEngine = EmbeddingEngine(applicationContext)\n        ttsEngine = TTSEngine(applicationContext)')
    
    content = content.replace('ChatViewModelFactory(repository, settingsRepository, memoryDao, embeddingEngine)', 'ChatViewModelFactory(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine)')
    
    content = content.replace('super.onDestroy()', 'ttsEngine.shutdown()\n        super.onDestroy()')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
