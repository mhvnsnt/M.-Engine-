import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun clearMemory() {
        viewModelScope.launch {
            memoryDao.deleteAllFragments()
        }
    }"""

replacement = """    fun clearMemory() {
        viewModelScope.launch {
            memoryDao.deleteAllFragments()
        }
    }
    
    fun clearCoreMemory() {
        viewModelScope.launch {
            memoryDao.deleteFragmentsByType("CORE")
        }
    }

    fun clearEpisodicMemory() {
        viewModelScope.launch {
            memoryDao.deleteFragmentsByType("EPISODIC")
        }
    }

    fun clearAllRegionProfiles() {
        viewModelScope.launch {
            locationRepository.deleteAllRegions()
        }
    }
    
    fun clearLocationSnapshots() {
        viewModelScope.launch {
            locationRepository.deleteSnapshots()
        }
    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
