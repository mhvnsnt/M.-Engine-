import re

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "r") as f:
    content = f.read()

target1 = "    companion object {"
new1 = """    companion object {
        val TELEGRAM_BOT_TOKEN = stringPreferencesKey("telegram_bot_token")"""
content = content.replace(target1, new1)

target2 = "    val openRouterKeyFlow: Flow<String> = dataStore.data.map { preferences ->"
new2 = """    val telegramBotTokenFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[TELEGRAM_BOT_TOKEN] ?: ""
    }
    
    suspend fun updateTelegramBotToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TELEGRAM_BOT_TOKEN] = token
        }
    }
    
    val openRouterKeyFlow: Flow<String> = dataStore.data.map { preferences ->"""
content = content.replace(target2, new2)

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "w") as f:
    f.write(content)
