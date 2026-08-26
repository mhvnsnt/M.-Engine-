with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        val astroRepository = com.example.data.AstroNumerologyRepository(database.astroDao())"""
replacement = """        val astroRepository = com.example.data.AstroNumerologyRepository(database.astroDao())
        val localIntelligenceRepository = com.example.data.LocalIntelligenceRepository(applicationContext)"""
content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
