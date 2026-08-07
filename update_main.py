with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'ChatViewModelFactory(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine)',
    'ChatViewModelFactory(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, applicationContext)'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
