import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """            val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\\n") { it.text }
            val currentWorkspace = workspaceContext.value
            
            var finalSystemInstruction = currentInstruction + profileContext + ragContext"""

replacement = """            val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\\n") { it.text }
            val currentWorkspace = workspaceContext.value
            
            // Location and Constraints
            val currentRegion = locationRepository.fetchCurrentLocationAndRegion()
            val constraints = locationRepository.userConstraintsFlow.firstOrNull()
            
            var locationContext = ""
            if (currentRegion != null) {
                locationContext += "\\n\\n[REGION CONTEXT]\\nActive Region: ${currentRegion.displayName}\\nLocal Notes: ${currentRegion.localNotes}"
            }
            if (constraints != null) {
                locationContext += "\\n\\n[USER CONSTRAINTS (HARD FILTERS)]\\nBudget Mode: ${constraints.budgetMode}\\nEntry Cost: ${constraints.entryCostFilter}\\nRole: ${constraints.userRole}\\nExcluded: ${constraints.excludedCategories}\\nDo not suggest anything violating these constraints."
            }
            
            // Astro Context
            val astroProfile = astroRepository.astroProfileFlow.firstOrNull()
            var astroContext = ""
            if (astroProfile != null) {
                astroContext += "\\n\\n[ASTRO & NUMEROLOGY BLUEPRINT]\\nLife Path: ${astroProfile.lifePathNumber}, Expression: ${astroProfile.expressionNumber}\\nPlacements: ${astroProfile.placementsJson}\\n"
                astroContext += astroRepository.getCurrentTransitsContext()
            }
            
            var finalSystemInstruction = currentInstruction + profileContext + ragContext + locationContext + astroContext"""

content = content.replace(target, replacement)

# Add import for firstOrNull
if "import kotlinx.coroutines.flow.firstOrNull" not in content:
    content = content.replace("import kotlinx.coroutines.flow.first", "import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.flow.firstOrNull")

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
