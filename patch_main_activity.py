with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        val astroRepository = com.example.data.AstroNumerologyRepository(applicationContext)"""
replacement = """        val astroRepository = com.example.data.AstroNumerologyRepository(applicationContext)
        val localIntelligenceRepository = com.example.data.LocalIntelligenceRepository(applicationContext)"""
content = content.replace(target, replacement)

target2 = """        viewModel = ViewModelProvider(this, ChatViewModelFactory(locationRepository, astroRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, applicationContext))[ChatViewModel::class.java]"""
replacement2 = """        viewModel = ViewModelProvider(this, ChatViewModelFactory(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, applicationContext))[ChatViewModel::class.java]"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
