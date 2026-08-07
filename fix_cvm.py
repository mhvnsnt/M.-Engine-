import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Add TTSEngine and WorkspaceContext to ChatViewModel
if 'private val ttsEngine: com.example.ai.TTSEngine' not in content:
    content = content.replace(
        'private val embeddingEngine: EmbeddingEngine',
        'private val embeddingEngine: EmbeddingEngine,\n    private val ttsEngine: com.example.ai.TTSEngine'
    )
    
    content = content.replace(
        'val systemInstruction: StateFlow<String> =',
        'val workspaceContext = MutableStateFlow<String?>("")\n\n    val systemInstruction: StateFlow<String> ='
    )

    content = content.replace(
        'class ChatViewModelFactory(',
        'class ChatViewModelFactory(\n    private val ttsEngine: com.example.ai.TTSEngine,'
    )
    
    content = content.replace(
        'return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine) as T',
        'return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine) as T'
    )

# When constructing system prompt, include Core Memory and Workspace Context
if 'val coreMemory =' not in content:
    # We find where finalSystemInstruction is built, in `sendMessage`
    system_build_regex = r'val currentInstruction = systemInstruction\.value\s+val finalSystemInstruction = if \(profile != null\) \{\s+"\$currentInstruction\\n\\nAdopt the following style:\\n\$\{profile\.prompt\}"\s+\} else \{\s+currentInstruction\s+\}'
    
    new_system_build = """val currentInstruction = systemInstruction.value
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
    
    content = re.sub(system_build_regex, new_system_build, content, flags=re.MULTILINE | re.DOTALL)


# After text generation is completed, trigger TTS
if 'ttsEngine.speak(' not in content:
    speak_regex = r'(_isGenerating\.value = false\s+repository\.insertMessage\(MessageEntity\(text = aiResponse\.trim\(\), timestamp = System\.currentTimeMillis\(\), isUser = false, responderName = endpoint\.name\)\))'
    new_speak = r'\1\n                ttsEngine.speak(aiResponse.trim())'
    content = re.sub(speak_regex, new_speak, content, flags=re.MULTILINE)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

