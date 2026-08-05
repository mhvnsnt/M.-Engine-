package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.EndpointEntity
import com.example.data.MessageEntity
import com.example.data.SettingsRepository
import com.example.data.StyleProfileEntity
import com.example.network.OllamaChatRequest
import com.example.network.OllamaChatResponse
import com.example.network.OllamaMessage
import com.example.network.OpenRouterRequest
import com.example.network.OpenRouterResponse
import com.example.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import com.example.network.DeviceCodeResponse
import com.example.network.AccessTokenResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

import com.example.ai.EmbeddingEngine
import com.example.data.MemoryFragment
import com.example.data.MemoryFragmentDao


data class DeviceFlowState(
    val userCode: String = "",
    val verificationUri: String = "",
    val isPolling: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine
) : ViewModel() {
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val styleProfile: StateFlow<StyleProfileEntity?> = repository.styleProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val endpoints: StateFlow<List<EndpointEntity>> = repository.allEndpoints.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val systemInstruction: StateFlow<String> = settingsRepository.systemInstructionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_SYSTEM_INSTRUCTION
    )


    val githubClientId: StateFlow<String> = settingsRepository.githubClientIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    private val _deviceFlowState = MutableStateFlow<DeviceFlowState?>(null)
    val deviceFlowState: StateFlow<DeviceFlowState?> = _deviceFlowState

    val githubPat: StateFlow<String> = settingsRepository.githubPatFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val useWhisperModel: StateFlow<Boolean> = settingsRepository.useWhisperModelFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val voiceAdaptation: StateFlow<Boolean> = settingsRepository.voiceAdaptationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val transcriptionLanguage: StateFlow<String> = settingsRepository.transcriptionLanguageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val responseAdapter = moshi.adapter(OllamaChatResponse::class.java)
    private val openRouterResponseAdapter = moshi.adapter(OpenRouterResponse::class.java)

    init {
        viewModelScope.launch {
            if (repository.getEndpointCount() == 0) {
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = true
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Llama 3)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "llama3-8b-8192",
                    type = "OPENAI",
                    isActive = false,
                    isPrimary = false
                ))
            }
        }
    }

    fun updateSystemInstruction(instruction: String) {
        viewModelScope.launch {
            settingsRepository.updateSystemInstruction(instruction)
        }
    }


    fun updateGithubClientId(clientId: String) {
        viewModelScope.launch {
            settingsRepository.updateGithubClientId(clientId)
        }
    }

    fun startGithubDeviceFlow(clientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _deviceFlowState.value = DeviceFlowState(isPolling = true)
                val response = RetrofitClient.githubAuthService.getDeviceCode(clientId = clientId)
                _deviceFlowState.value = DeviceFlowState(
                    userCode = response.user_code,
                    verificationUri = response.verification_uri,
                    isPolling = true
                )
                
                var token: String? = null
                var interval = response.interval.toLong() * 1000L
                val expiresAt = System.currentTimeMillis() + response.expires_in * 1000L
                
                while (System.currentTimeMillis() < expiresAt && _deviceFlowState.value?.isPolling == true) {
                    delay(interval)
                    val tokenResponse = RetrofitClient.githubAuthService.getAccessToken(
                        clientId = clientId,
                        deviceCode = response.device_code
                    )
                    
                    if (tokenResponse.access_token != null) {
                        token = tokenResponse.access_token
                        break
                    } else if (tokenResponse.error == "authorization_pending") {
                        // Keep polling
                    } else if (tokenResponse.error == "slow_down") {
                        interval += 5000L
                    } else {
                        _deviceFlowState.value = _deviceFlowState.value?.copy(
                            error = tokenResponse.error_description ?: "Authentication failed",
                            isPolling = false
                        )
                        return@launch
                    }
                }
                
                if (token != null) {
                    updateGithubPat(token)
                    _deviceFlowState.value = null // Close dialog
                } else if (_deviceFlowState.value?.isPolling == true) {
                    _deviceFlowState.value = _deviceFlowState.value?.copy(
                        error = "Authentication timed out",
                        isPolling = false
                    )
                }
            } catch (e: Exception) {
                _deviceFlowState.value = _deviceFlowState.value?.copy(
                    error = e.message ?: "Network error",
                    isPolling = false
                )
            }
        }
    }

    fun cancelGithubDeviceFlow() {
        _deviceFlowState.value = null
    }

    fun updateGithubPat(pat: String) {
        viewModelScope.launch {
            settingsRepository.updateGithubPat(pat)
        }
    }

    fun updateUseWhisperModel(use: Boolean) {
        viewModelScope.launch { settingsRepository.updateUseWhisperModel(use) }
    }
    fun updateVoiceAdaptation(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateVoiceAdaptation(enabled) }
    }
    fun updateTranscriptionLanguage(lang: String) {
        viewModelScope.launch { settingsRepository.updateTranscriptionLanguage(lang) }
    }

    fun addEndpoint(name: String, url: String, apiKey: String, modelName: String, type: String) {
        viewModelScope.launch {
            repository.insertEndpoint(EndpointEntity(
                name = name,
                url = url,
                apiKey = apiKey,
                modelName = modelName,
                type = type,
                isActive = true,
                isPrimary = repository.getEndpointCount() == 0
            ))
        }
    }

    fun toggleEndpointActive(endpoint: EndpointEntity, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateEndpoint(endpoint.copy(isActive = isActive))
        }
    }

    fun deleteEndpoint(endpoint: EndpointEntity) {
        viewModelScope.launch {
            repository.deleteEndpoint(endpoint)
        }
    }

    fun setPrimaryEndpoint(endpoint: EndpointEntity) {
        viewModelScope.launch {
            repository.getPrimaryEndpoint()?.let {
                repository.updateEndpoint(it.copy(isPrimary = false))
            }
            repository.updateEndpoint(endpoint.copy(isPrimary = true))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val currentInstruction = systemInstruction.value
        
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            val groupId = System.currentTimeMillis()
            
            // Save user message and embed
            val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId)
            repository.insertMessage(userMsg)
            
            try {
                val embedding = embeddingEngine.generateEmbedding(text)
                memoryDao.insert(MemoryFragment(text = text, timestamp = groupId, isUser = true, embedding = embedding.joinToString(",")))
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
            val currentMessages = repository.allMessages.stateIn(viewModelScope).value
            val history = mutableListOf<OllamaMessage>()
            
            var ragContext = ""
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
                    ragContext = "\n\n[RETRIEVED MEMORIES]\n" + nearest.joinToString("\n") { (if(it.isUser) "User" else "Assistant") + ": " + it.text }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val profileContext = if (updatedProfile.totalMessages > 0) {
                val avgLength = updatedProfile.totalWords / updatedProfile.totalMessages
                "\n\n[LOCAL MEMORY CONTEXT]\nThe user's average sentence length is $avgLength words. " +
                "They frequently discuss topics: ${updatedProfile.topics}. " +
                "Adapt your response style to mirror their cadence and vocabulary. Maintain an identical tone."
            } else ""

            val finalSystemInstruction = currentInstruction + profileContext + ragContext
            if (finalSystemInstruction.isNotBlank()) {
                history.add(OllamaMessage(role = "system", content = finalSystemInstruction))
            }
            
            currentMessages.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }
            history.add(OllamaMessage(role = "user", content = text))
            
            val activeEndpoints = repository.getActiveEndpoints()
            if (activeEndpoints.isEmpty()) {
                _errorMessage.value = "No active endpoints found."
                _isGenerating.value = false
                return@launch
            }
            
            val jobs = activeEndpoints.map { endpoint ->
                async {
                    if (endpoint.type == "OLLAMA") {
                        streamOllamaModel(endpoint, history, groupId)
                    } else {
                        streamOpenRouterModel(endpoint, history, groupId)
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
    
    private suspend fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long) {
        val request = OpenRouterRequest(model = endpoint.modelName, messages = history, stream = true)
        val placeholderMsg = MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId)
        val insertedId = repository.insertMessage(placeholderMsg).toInt()
        
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.openRouterService.generateChatStream(
                    url = endpoint.url,
                    authHeader = "Bearer ${endpoint.apiKey}",
                    request = request
                )
                val reader = BufferedReader(InputStreamReader(response.byteStream()))
                
                var completeResponse = ""
                var lastUpdateTime = System.currentTimeMillis()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { rawLine ->
                        if (rawLine.startsWith("data: ")) {
                            val jsonLine = rawLine.substring(6)
                            if (jsonLine == "[DONE]") return@let
                            try {
                                val chunk = openRouterResponseAdapter.fromJson(jsonLine)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { contentChunk ->
                                    completeResponse += contentChunk
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdateTime > 50) {
                                        repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                        lastUpdateTime = now
                                    }
                                }
                            } catch (e: Exception) { }
                        }
                    }
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                try {
                    val embedding = embeddingEngine.generateEmbedding(completeResponse)
                    memoryDao.insert(MemoryFragment(text = completeResponse, timestamp = groupId, isUser = false, embedding = embedding.joinToString(",")))
                } catch (e: Exception) { e.printStackTrace() }
            } catch (e: Exception) {
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
            }
        }
    }
    
    private suspend fun streamOllamaModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long) {
        val request = OllamaChatRequest(model = endpoint.modelName, messages = history, stream = true)
        val placeholderMsg = MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId)
        val insertedId = repository.insertMessage(placeholderMsg).toInt()
        
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.service.generateChatStream(endpoint.url, request)
                val reader = BufferedReader(InputStreamReader(response.byteStream()))
                
                var completeResponse = ""
                var lastUpdateTime = System.currentTimeMillis()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { jsonLine ->
                        try {
                            val chunk = responseAdapter.fromJson(jsonLine)
                            chunk?.message?.content?.let { contentChunk ->
                                completeResponse += contentChunk
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 50 || chunk.done) {
                                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                    lastUpdateTime = now
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                try {
                    val embedding = embeddingEngine.generateEmbedding(completeResponse)
                    memoryDao.insert(MemoryFragment(text = completeResponse, timestamp = groupId, isUser = false, embedding = embedding.joinToString(",")))
                } catch (e: Exception) { e.printStackTrace() }
            } catch (e: Exception) {
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: ${e.message}"))
            }
        }
    }
    
    fun synthesizeCouncilOutputs(messages: List<MessageEntity>) {
        if (messages.isEmpty()) return
        
        val prompt = "Please synthesize the following AI responses into a final consensus:\n\n" +
            messages.joinToString("\n\n---\n\n") { "Model: ${it.responderName}\n${it.text}" }

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null

            val primary = repository.getPrimaryEndpoint()
            if (primary == null) {
                _errorMessage.value = "No primary endpoint selected for synthesis."
                _isGenerating.value = false
                return@launch
            }
            
            val currentMessages = repository.allMessages.stateIn(viewModelScope).value
            val history = mutableListOf<OllamaMessage>()
            
            val currentInstruction = systemInstruction.value
            val currentProfile = repository.styleProfile.stateIn(viewModelScope).value ?: StyleProfileEntity()
            val profileContext = if (currentProfile.totalMessages > 0) {
                val avgLength = currentProfile.totalWords / currentProfile.totalMessages
                "\n\n[LOCAL MEMORY CONTEXT]\nThe user's average sentence length is $avgLength words. " +
                "They frequently discuss topics: ${currentProfile.topics}. " +
                "Adapt your response style to mirror their cadence and vocabulary. Maintain an identical tone."
            } else ""
            
            val finalSystemInstruction = currentInstruction + profileContext
            if (finalSystemInstruction.isNotBlank()) {
                history.add(OllamaMessage(role = "system", content = finalSystemInstruction))
            }

            // Include history up to the original user prompt for context
            val groupId = messages.first().groupId
            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }
            
            history.add(OllamaMessage(role = "user", content = prompt))
            
            val newGroupId = System.currentTimeMillis()
            if (primary.type == "OLLAMA") {
                streamOllamaModel(primary, history, newGroupId)
            } else {
                streamOpenRouterModel(primary, history, newGroupId)
            }
            
            _isGenerating.value = false
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            repository.clearMessages()
            repository.clearProfile()
        }
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
