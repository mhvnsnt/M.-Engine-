import re

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "r") as f:
    content = f.read()

# Add import
if "import com.example.BuildConfig" not in content:
    content = content.replace("import kotlinx.coroutines.flow.map", "import kotlinx.coroutines.flow.map\nimport com.example.BuildConfig")

# Update githubPatFlow to use BuildConfig fallback
target = """    val githubPatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[GITHUB_PAT]
        if (encryptedKey != null) {
            try {
                String(CryptoManager.decrypt(encryptedKey))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }"""
    
replacement = """    val githubPatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedKey = preferences[GITHUB_PAT]
        val localKey = if (encryptedKey != null) {
            try {
                String(CryptoManager.decrypt(encryptedKey))
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
        
        if (localKey.isEmpty() && BuildConfig.Mengine_Github_PAT.isNotEmpty()) {
            BuildConfig.Mengine_Github_PAT
        } else {
            localKey
        }
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "w") as f:
    f.write(content)

