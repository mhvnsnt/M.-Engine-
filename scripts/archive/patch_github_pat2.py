import re

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "r") as f:
    content = f.read()

target = """        if (localKey.isEmpty() && BuildConfig.Mengine_Github_PAT.isNotEmpty()) {
            BuildConfig.Mengine_Github_PAT
        } else {
            localKey
        }"""
        
replacement = """        if (localKey.isEmpty() && BuildConfig.Mengine_Github_PAT != "DEFAULT_PAT" && BuildConfig.Mengine_Github_PAT.isNotEmpty()) {
            BuildConfig.Mengine_Github_PAT
        } else {
            localKey
        }"""
        
content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/SettingsRepository.kt", "w") as f:
    f.write(content)
