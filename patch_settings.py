import re

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "r") as f:
    content = f.read()

# Add telegram bot token flow if missing
if 'val telegramBotTokenFlow' not in content:
    token_key = 'val TELEGRAM_BOT_TOKEN = stringPreferencesKey("telegram_bot_token")'
    if token_key not in content:
        content = content.replace('companion object {', f'companion object {{\n        {token_key}')
    
    flow_code = """    val telegramBotTokenFlow = dataStore.data.map { preferences ->
        preferences[TELEGRAM_BOT_TOKEN] ?: ""
    }
    
    suspend fun updateTelegramBotToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TELEGRAM_BOT_TOKEN] = token
        }
    }
    
"""
    content = content.replace('val openRouterKeyFlow', flow_code + '    val openRouterKeyFlow')

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "w") as f:
    f.write(content)
