import re

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "r") as f:
    content = f.read()

if "val AUTO_SYNC_GITHUB =" not in content:
    content = content.replace('val GITHUB_CLIENT_ID = stringPreferencesKey("github_client_id")', 'val GITHUB_CLIENT_ID = stringPreferencesKey("github_client_id")\n        val AUTO_SYNC_GITHUB = booleanPreferencesKey("auto_sync_github")\n        val PULL_MEMORY_ON_START = booleanPreferencesKey("pull_memory_on_start")')
    
    content = content.replace('val githubPatFlow: Flow<String>', 'val autoSyncGithubFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_SYNC_GITHUB] ?: false }\n    val pullMemoryOnStartFlow: Flow<Boolean> = context.dataStore.data.map { it[PULL_MEMORY_ON_START] ?: false }\n\n    val githubPatFlow: Flow<String>')

    methods = """    suspend fun updateAutoSyncGithub(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_GITHUB] = value
        }
    }
    
    suspend fun updatePullMemoryOnStart(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PULL_MEMORY_ON_START] = value
        }
    }
"""
    content = content.replace("suspend fun updateGithubPat(key: String) {", methods + "\n    suspend fun updateGithubPat(key: String) {")

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "w") as f:
    f.write(content)

