#!/bin/bash
set -e

# Update ChatViewModelFactory signature in MainActivity and ChatViewModel
sed -i 's/class ChatViewModel(/class ChatViewModel(\n    private val locationRepository: com.example.data.LocationRepository,\n    private val astroRepository: com.example.data.AstroNumerologyRepository,\n/' app/src/main/java/com/example/ui/ChatViewModel.kt

sed -i 's/class ChatViewModelFactory(/class ChatViewModelFactory(\n    private val locationRepository: com.example.data.LocationRepository,\n    private val astroRepository: com.example.data.AstroNumerologyRepository,\n/' app/src/main/java/com/example/ui/ChatViewModel.kt

sed -i 's/return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T/return ChatViewModel(locationRepository, astroRepository, repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T/' app/src/main/java/com/example/ui/ChatViewModel.kt

# Initialize repositories in MainActivity
sed -i '/settingsRepository = SettingsRepository(applicationContext)/a \        val locationRepository = com.example.data.LocationRepository(applicationContext, database.locationDao())\n        val astroRepository = com.example.data.AstroNumerologyRepository(database.astroDao())' app/src/main/java/com/example/MainActivity.kt

sed -i 's/ChatViewModelFactory(repository/ChatViewModelFactory(locationRepository, astroRepository, repository/' app/src/main/java/com/example/MainActivity.kt

