import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# When constructing system prompt, include Core Memory and Workspace Context
if 'val coreMemory =' not in content:
    # We find where finalSystemInstruction is built, in `generateAiResponse` or something?
    # Let's just find `val currentInstruction = systemInstruction.value` inside the functions
    old_code = """        val currentInstruction = systemInstruction.value
        
        val finalSystemInstruction = if (profile != null) {
            "$currentInstruction\\n\\nAdopt the following style:\\n${profile.prompt}"
        } else {
            currentInstruction
        }"""
        
    new_code = """        val currentInstruction = systemInstruction.value
        val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\\n") { it.text }
        val currentWorkspace = workspaceContext.value
        
        var finalSystemInstruction = currentInstruction
        if (coreMemory.isNotBlank()) {
            finalSystemInstruction += "\\n\\n[CORE MEMORY]\\n$coreMemory"
        }
        if (!currentWorkspace.isNullOrBlank()) {
            finalSystemInstruction += "\\n\\n[CURRENT WORKSPACE FILE CONTEXT]\\nThe user is currently viewing this file:\\n```\\n$currentWorkspace\\n```"
        }
        if (profile != null) {
            finalSystemInstruction += "\\n\\nAdopt the following style:\\n${profile.prompt}"
        }"""
        
    content = content.replace(old_code, new_code)
    
    # there might be another occurrence
    content = content.replace(
        "val currentInstruction = systemInstruction.value\n                val finalSystemInstruction = if (profile != null) {\n                    \"$currentInstruction\\n\\nAdopt the following style:\\n${profile.prompt}\"\n                } else {\n                    currentInstruction\n                }",
        new_code
    )

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

