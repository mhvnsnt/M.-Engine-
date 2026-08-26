with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """            var locationContext = ""
            if (currentRegion != null) {
                locationContext += "\n\n[REGION CONTEXT]\nActive Region: ${currentRegion.displayName}\nLocal Notes: ${currentRegion.localNotes}"
            }"""
replacement = """            var locationContext = ""
            if (currentRegion != null) {
                locationContext += "\n\n[REGION CONTEXT]\nActive Region: ${currentRegion.displayName}\nLocal Notes: ${currentRegion.localNotes}"
                
                val intel = localIntelligenceRepository.getSceneContext(currentRegion.regionId)
                locationContext += "\n\n$intel"
            }"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
