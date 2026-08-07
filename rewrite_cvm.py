import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Fix `sendMessage`
start_sm = content.find('fun sendMessage(')
end_sm = content.find('private suspend fun streamOpenRouterModel', start_sm)

new_send_message = """    fun sendMessage(text: String, imageUri: String? = null) {
        val sessionId = _currentSessionId.value ?: return
        if (text.isBlank() && imageUri == null) return
        
        val currentInstruction = systemInstruction.value
        
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            val groupId = System.currentTimeMillis()
            
            // Save user message and embed
            val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri, sessionId = sessionId)
            repository.insertMessage(userMsg)
            
            try {
                if (text.isNotBlank()) {
                    val embedding = embeddingEngine.generateEmbedding(text)
                    memoryDao.insert(MemoryFragment(text = text, timestamp = groupId, isUser = true, embedding = embedding.joinToString(",")))
                }
            } catch (e: Exception) {
                e.printStackTrace() // Ignore embedding failures
            }
            
            // Extract and update style profile
            val currentProfile = repository.styleProfile.stateIn(viewModelScope).value ?: StyleProfileEntity()
            val words = text.split(Regex("\\s+"))
            val wordCount = words.size
            val newTopics = words.filter { it.length > 5 && it[0].isUpperCase() }.take(3).joinToString(", ")
            val updatedTopics = if (currentProfile.topics.isEmpty()) newTopics else "${currentProfile.topics}, $newTopics".split(", ").filter { it.isNotBlank() }.distinct().take(10).joinToString(", ")
            
            val updatedProfile = currentProfile.copy(
                totalMessages = currentProfile.totalMessages + 1,
                totalWords = currentProfile.totalWords + wordCount,
                topics = updatedTopics
            )
            repository.saveProfile(updatedProfile)
            
            // Construct request
            val currentMessages = repository.getMessagesForSession(sessionId).stateIn(viewModelScope).value ?: emptyList()
            val ollamaHistory = mutableListOf<OllamaMessage>()
            val openRouterHistory = mutableListOf<OpenRouterMessage>()
            
            var ragContext = ""
            if (text.isNotBlank()) {
                try {
                    val currentEmbedding = embeddingEngine.generateEmbedding(text)
                    
                    val allMemories = memoryDao.getAllFragments()
                    val nearest = allMemories.mapNotNull { mem ->
                        if (mem.embedding.isBlank()) return@mapNotNull null
                        val emb = mem.embedding.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                        if (emb.size != currentEmbedding.size) return@mapNotNull null
                        var dotProduct = 0f
                        for (i in emb.indices) { dotProduct += emb[i] * currentEmbedding[i] }
                        mem to dotProduct
                    }.sortedByDescending { it.second }.take(3).map { it.first }
                    if (nearest.isNotEmpty()) {
                        ragContext = "\\n\\n[RETRIEVED MEMORIES]\\n" + nearest.joinToString("\\n") { (if(it.isUser) "User" else "Assistant") + ": " + it.text }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val profileContext = if (updatedProfile.totalMessages > 0) {
                val avgLength = updatedProfile.totalWords / updatedProfile.totalMessages
                "\\n\\n[LOCAL MEMORY CONTEXT]\\nThe user's average sentence length is $avgLength words. " +
                "They frequently discuss topics: ${updatedProfile.topics}. " +
                "Adapt your response style to mirror their cadence and vocabulary. Maintain an identical tone."
            } else ""
            
            val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\\n") { it.text }
            val currentWorkspace = workspaceContext.value
            
            var finalSystemInstruction = currentInstruction + profileContext + ragContext
            if (coreMemory.isNotBlank()) {
                finalSystemInstruction += "\\n\\n[CORE MEMORY]\\n$coreMemory"
            }
            if (!currentWorkspace.isNullOrBlank()) {
                finalSystemInstruction += "\\n\\n[CURRENT WORKSPACE FILE CONTEXT]\\nThe user is currently viewing this file:\\n```\\n$currentWorkspace\\n```"
            }
            if (finalSystemInstruction.isNotBlank()) {
                ollamaHistory.add(OllamaMessage(role = "system", content = finalSystemInstruction))
                openRouterHistory.add(OpenRouterMessage(role = "system", content = finalSystemInstruction))
            }
            
            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                val role = if (msg.isUser) "user" else "assistant"
                ollamaHistory.add(OllamaMessage(role = role, content = msg.text))
                if (msg.imageUri != null) {
                    val base64 = getBase64FromUri(Uri.parse(msg.imageUri))
                    if (base64 != null) {
                        val parts = listOf(
                            OpenRouterContentPart(type = "text", text = msg.text),
                            OpenRouterContentPart(type = "image_url", image_url = OpenRouterImageUrl(url = base64))
                        )
                        openRouterHistory.add(OpenRouterMessage(role = role, content = parts))
                    } else {
                        openRouterHistory.add(OpenRouterMessage(role = role, content = msg.text))
                    }
                } else {
                    openRouterHistory.add(OpenRouterMessage(role = role, content = msg.text))
                }
            }
            
            val finalPrompt = if (ragContext.isNotBlank()) "Context:\\n$ragContext\\n\\nPrompt:\\n$text" else text
            ollamaHistory.add(OllamaMessage(role = "user", content = finalPrompt))
            
            if (imageUri != null) {
                val base64 = getBase64FromUri(Uri.parse(imageUri))
                if (base64 != null) {
                    val parts = listOf(
                        OpenRouterContentPart(type = "text", text = finalPrompt),
                        OpenRouterContentPart(type = "image_url", image_url = OpenRouterImageUrl(url = base64))
                    )
                    openRouterHistory.add(OpenRouterMessage(role = "user", content = parts))
                } else {
                    openRouterHistory.add(OpenRouterMessage(role = "user", content = finalPrompt))
                }
            } else {
                openRouterHistory.add(OpenRouterMessage(role = "user", content = finalPrompt))
            }
            
            val activeEndpoints = repository.getActiveEndpoints()
            if (activeEndpoints.isEmpty()) {
                _errorMessage.value = "No active endpoints found."
                _isGenerating.value = false
                return@launch
            }
            
            val jobs = activeEndpoints.map { endpoint ->
                async {
                    if (endpoint.type == "OLLAMA") {
                        streamOllamaModel(endpoint, ollamaHistory, groupId, sessionId)
                    } else {
                        streamOpenRouterModel(endpoint, openRouterHistory, groupId, sessionId)
                    }
                }
            }
            try {
                jobs.awaitAll()
            } catch (e: Exception) {
                _errorMessage.value = "Council Error: ${e.message}"
            }
            
            _isGenerating.value = false
        }
    }
"""
content = content[:start_sm] + new_send_message + content[end_sm:]

# Now replace synthesizeCouncilOutputs
start_sync = content.find('fun synthesizeCouncilOutputs(')
end_sync = content.find('fun clearMemory()', start_sync)

new_sync = """    fun synthesizeCouncilOutputs(messages: List<MessageEntity>) {
        if (messages.isEmpty()) return
        
        val prompt = "Please synthesize the following AI responses into a final consensus:\\n\\n" +
            messages.joinToString("\\n\\n---\\n\\n") { "Model: ${it.responderName}\\n${it.text}" }

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null

            val primary = repository.getPrimaryEndpoint()
            if (primary == null) {
                _errorMessage.value = "No primary endpoint selected for synthesis."
                _isGenerating.value = false
                return@launch
            }
            
            val sessionId = _currentSessionId.value ?: 1L
            val currentMessages = repository.getMessagesForSession(sessionId).stateIn(viewModelScope).value ?: emptyList()
            val ollamaHistory = mutableListOf<OllamaMessage>()
            val openRouterHistory = mutableListOf<OpenRouterMessage>()
            
            val currentInstruction = systemInstruction.value
            val currentProfile = repository.styleProfile.stateIn(viewModelScope).value ?: StyleProfileEntity()
            val profileContext = if (currentProfile.totalMessages > 0) {
                val avgLength = currentProfile.totalWords / currentProfile.totalMessages
                "\\n\\n[LOCAL MEMORY CONTEXT]\\nThe user's average sentence length is $avgLength words. " +
                "They frequently discuss topics: ${currentProfile.topics}. " +
                "Adapt your response style to mirror their cadence and vocabulary. Maintain an identical tone."
            } else ""
            
            val finalSystemInstruction = currentInstruction + profileContext
            if (finalSystemInstruction.isNotBlank()) {
                ollamaHistory.add(OllamaMessage(role = "system", content = finalSystemInstruction))
                openRouterHistory.add(OpenRouterMessage(role = "system", content = finalSystemInstruction))
            }

            // Include history up to the original user prompt for context
            val groupId = messages.first().groupId
            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                val role = if (msg.isUser) "user" else "assistant"
                ollamaHistory.add(OllamaMessage(role = role, content = msg.text))
                openRouterHistory.add(OpenRouterMessage(role = role, content = msg.text))
            }
            
            ollamaHistory.add(OllamaMessage(role = "user", content = prompt))
            openRouterHistory.add(OpenRouterMessage(role = "user", content = prompt))
            
            val newGroupId = System.currentTimeMillis()
            
            if (primary.type == "OLLAMA") {
                streamOllamaModel(primary, ollamaHistory, newGroupId, sessionId)
            } else {
                streamOpenRouterModel(primary, openRouterHistory, newGroupId, sessionId)
            }
        }
    }

"""
content = content[:start_sync] + new_sync + content[end_sync:]

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

