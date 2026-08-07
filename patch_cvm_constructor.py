with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """class ChatViewModel(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,"""
replacement = """class ChatViewModel(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,
    private val localIntelligenceRepository: com.example.data.LocalIntelligenceRepository,"""
content = content.replace(target, replacement)

target2 = """class ChatViewModelFactory(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,"""
replacement2 = """class ChatViewModelFactory(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,
    private val localIntelligenceRepository: com.example.data.LocalIntelligenceRepository,"""
content = content.replace(target2, replacement2)

target3 = """        return ChatViewModel(locationRepository, astroRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T"""
replacement3 = """        return ChatViewModel(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
