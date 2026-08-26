import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target1 = "ChatViewModelFactory(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, applicationContext)"
new1 = "ChatViewModelFactory(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, database.graphNodeDao(), embeddingEngine, ttsEngine, applicationContext)"
content = content.replace(target1, new1)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
