import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

old_code = "val finalSystemInstruction = currentInstruction + profileContext + ragContext"
new_code = """val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\\n") { it.text }
            val currentWorkspace = workspaceContext.value
            
            var finalSystemInstruction = currentInstruction + profileContext + ragContext
            if (coreMemory.isNotBlank()) {
                finalSystemInstruction += "\\n\\n[CORE MEMORY]\\n$coreMemory"
            }
            if (!currentWorkspace.isNullOrBlank()) {
                finalSystemInstruction += "\\n\\n[CURRENT WORKSPACE FILE CONTEXT]\\nThe user is currently viewing this file:\\n```\\n$currentWorkspace\\n```"
            }"""

content = content.replace(old_code, new_code)
with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

