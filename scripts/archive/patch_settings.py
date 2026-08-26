with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'r') as f:
    content = f.read()

target = """        val TRANSCRIPTION_LANGUAGE = stringPreferencesKey("transcription_language")"""
replacement = """        val TRANSCRIPTION_LANGUAGE = stringPreferencesKey("transcription_language")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")"""
content = content.replace(target, replacement)

target2 = """    val transcriptionLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCRIPTION_LANGUAGE] ?: "en"
    }"""
replacement2 = """    val transcriptionLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCRIPTION_LANGUAGE] ?: "en"
    }
    
    val isOnboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETE] ?: false
    }"""
content = content.replace(target2, replacement2)

target3 = """    suspend fun updateTranscriptionLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCRIPTION_LANGUAGE] = lang
        }
    }"""
replacement3 = """    suspend fun updateTranscriptionLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCRIPTION_LANGUAGE] = lang
        }
    }
    
    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETE] = true
        }
    }"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'w') as f:
    f.write(content)
