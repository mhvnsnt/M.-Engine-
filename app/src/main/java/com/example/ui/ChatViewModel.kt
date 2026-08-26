package com.example.ui

import androidx.lifecycle.ViewModel

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterImageUrl
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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.ai.GithubMonitorWorker
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
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
import com.example.github.HierarchicalMemoryManager
import com.example.ai.CodeJarvis
import com.example.ai.CodingTools


data class DeviceFlowState(
    val userCode: String = "",
    val verificationUri: String = "",
    val isPolling: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,
    private val localIntelligenceRepository: com.example.data.LocalIntelligenceRepository,

    private val repository: ChatRepository,
    val settingsRepository: SettingsRepository,
    private val memoryDao: MemoryFragmentDao,
    private val graphDao: com.example.data.GraphNodeDao,
    private val embeddingEngine: EmbeddingEngine,
    private val ttsEngine: com.example.ai.TTSEngine,
    private val context: android.content.Context
) : ViewModel() {

    val memoryManager = HierarchicalMemoryManager(context, memoryDao, embeddingEngine)
    val codingTools = CodingTools(context)
    val codeJarvis = CodeJarvis(codingTools, com.example.ai.TreeSitterEngine(), graphDao)
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

    val workspaceContext = MutableStateFlow<String?>("")

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
    private val _endpointStatuses = MutableStateFlow<Map<Int, String>>(emptyMap())
    val endpointStatuses: StateFlow<Map<Int, String>> = _endpointStatuses
    val deviceFlowState: StateFlow<DeviceFlowState?> = _deviceFlowState

    val autoSyncGithub: StateFlow<Boolean> = settingsRepository.autoSyncGithubFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val pullMemoryOnStart: StateFlow<Boolean> = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val telegramBotToken: StateFlow<String> = settingsRepository.telegramBotTokenFlow.stateIn(viewModelScope, SharingStarted.Lazily, "")
    
    fun updateTelegramBotToken(token: String) {
        viewModelScope.launch {
            settingsRepository.updateTelegramBotToken(token)
        }
    }
    val councilMode: StateFlow<Boolean> = settingsRepository.councilModeFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)

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


    private val treeSitterEngine = com.example.ai.TreeSitterEngine()
    private val reflectionEngine = com.example.ai.ReflectionEngine(memoryDao, graphDao, embeddingEngine, locationRepository)
    private val lindyEngine = com.example.ai.LindyEngine(settingsRepository.telegramBotTokenFlow, codeJarvis, settingsRepository.githubPatFlow, codingTools)

    init {

        reflectionEngine.startReflectionLoop()
        lindyEngine.startProactiveLoop { getPrimaryEndpointSync() }
        
        // Start proactive GitHub Action monitoring (Lindy background trigger)
        val workRequest = PeriodicWorkRequestBuilder<GithubMonitorWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "github_monitor",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        // Load system prompt from github if needed
        viewModelScope.launch {
            if (pullMemoryOnStart.value) {
                memoryManager.pullSystemPrompt(githubPat.value)
            }
            val prompt = memoryManager.getSystemPromptLocal()
            if (!prompt.isNullOrBlank() && systemInstruction.value == com.example.data.SettingsRepository.DEFAULT_SYSTEM_INSTRUCTION) {
                settingsRepository.updateSystemInstruction(prompt)
            } else if (!prompt.isNullOrBlank() && pullMemoryOnStart.value) {
                settingsRepository.updateSystemInstruction(prompt)
            }
        }

        viewModelScope.launch {
            val allEndpoints = repository.getAllEndpointsSync()
            val hasPollinations = allEndpoints.any { it.url.contains("pollinations") }
            if (!hasPollinations) {
                // Ensure no other endpoints are primary
                allEndpoints.forEach { 
                    if (it.isPrimary) repository.updateEndpoint(it.copy(isPrimary = false))
                }
                repository.insertEndpoint(com.example.data.EndpointEntity(
                    name = "Pollinations (GPT-OSS 20B)",
                    url = "https://text.pollinations.ai/openai/chat/completions",
                    apiKey = "", // No API key required!
                    modelName = "openai-fast",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
            } else {
                // Fix existing Pollinations endpoints that use the old legacy "llama" model which now returns 404
                allEndpoints.filter { it.url.contains("pollinations") && it.modelName == "llama" }.forEach {
                    repository.updateEndpoint(it.copy(modelName = "openai-fast", name = "Pollinations (GPT-OSS 20B)"))
                }
            }
            if (repository.getEndpointCount() <= 1) {
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Gemma)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Llama 3 Abliterated)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "llama3:8b-instruct-fp16",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Llama 3 8B)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "llama3-8b-8192",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Mixtral 8x7B)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "mixtral-8x7b-32768",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Dolphin Llama 3 8B Uncensored)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "cognitivecomputations/dolphin-llama-3-8b",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Hermes 3 8B Uncensored)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "nousresearch/hermes-3-llama-3.1-8b",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Google Gemini (Gemini 2.5 Flash Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "google/gemini-2.5-flash:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Llama 3.1 8B Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "meta-llama/llama-3.1-8b-instruct:free",
                    type = "OPENAI",
                    isActive = true,
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

        fun updateAutoSyncGithub(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSyncGithub(value)
        }
    }
    fun updatePullMemoryOnStart(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePullMemoryOnStart(value)
        }
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

    fun updateEndpointApiKey(id: Int, apiKey: String) {
        viewModelScope.launch {
            repository.updateEndpointApiKey(id, apiKey)
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

    fun sendMessage(text: String, imageUri: String? = null) {
        if (text.isBlank()) return
        
        val currentInstruction = systemInstruction.value
        
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            val groupId = System.currentTimeMillis()
            
            // Save user message and embed
            
            if (text.startsWith("/code ")) {
                val command = text.removePrefix("/code ").trim()
                val responseMsg = MessageEntity(text = "Executing CodeJarvis...", isUser = false, responderName = "CodeJarvis", groupId = groupId)
                val insertedId = repository.insertMessage(responseMsg).toInt()
                
                try {
                    val primaryEndpoint = repository.getPrimaryEndpoint()
                    if (primaryEndpoint != null) {
                        val result = codeJarvis.handleCodeCommand(
                            command = command,
                            githubPat = githubPat.value,
                            endpoint = primaryEndpoint
                        )
                        repository.updateMessage(responseMsg.copy(id = insertedId, text = result))
                    } else {
                        repository.updateMessage(responseMsg.copy(id = insertedId, text = "Error: No primary endpoint selected for CodeJarvis."))
                    }
                } catch(e: Exception) {
                    repository.updateMessage(responseMsg.copy(id = insertedId, text = "CodeJarvis Error: ${e.message}"))
                }
                syncMemory()
                return@launch
            }

            val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri)
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

            val coreMemory = memoryDao.getFragmentsByType("CORE").joinToString("\n") { it.text }
            val currentWorkspace = workspaceContext.value
            
            // Location and Constraints
            val currentRegion = locationRepository.fetchCurrentLocationAndRegion()
            val constraints = locationRepository.userConstraintsFlow.firstOrNull()
            
            var locationContext = ""
            if (currentRegion != null) {
                locationContext += "\n\n[REGION CONTEXT]\nActive Region: ${currentRegion.displayName}\nLocal Notes: ${currentRegion.localNotes}"
            }
            if (constraints != null) {
                locationContext += "\n\n[USER CONSTRAINTS (HARD FILTERS)]\nBudget Mode: ${constraints.budgetMode}\nEntry Cost: ${constraints.entryCostFilter}\nRole: ${constraints.userRole}\nExcluded: ${constraints.excludedCategories}\nDo not suggest anything violating these constraints."
            }
            
            // Astro Context
            val astroProfile = astroRepository.astroProfileFlow.firstOrNull()
            var astroContext = ""
            if (astroProfile != null) {
                astroContext += "\n\n[ASTRO & NUMEROLOGY BLUEPRINT]\nLife Path: ${astroProfile.lifePathNumber}, Expression: ${astroProfile.expressionNumber}\nPlacements: ${astroProfile.placementsJson}\n"
                astroContext += astroRepository.getCurrentTransitsContext()
            }
            
            var finalSystemInstruction = currentInstruction + profileContext + ragContext + locationContext + astroContext
            if (coreMemory.isNotBlank()) {
                finalSystemInstruction += "\n\n[CORE MEMORY]\n$coreMemory"
            }
            if (!currentWorkspace.isNullOrBlank()) {
                finalSystemInstruction += "\n\n[CURRENT WORKSPACE FILE CONTEXT]\nThe user is currently viewing this file:\n```\n$currentWorkspace\n```"
            }
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
                syncMemory()
                return@launch
            }
            
            if (councilMode.value) {
                val jobs = activeEndpoints.map { endpoint ->
                    async {
                        if (endpoint.type == "OLLAMA") {
                            streamOllamaModel(endpoint = endpoint, history = history, groupId = groupId, isLastFallback = true)
                        } else {
                            streamOpenRouterModel(endpoint = endpoint, history = history, groupId = groupId, isLastFallback = true)
                        }
                    }
                }
                try {
                    jobs.awaitAll()
                } catch (e: Exception) {
                    _errorMessage.value = "Council Error: ${e.message}"
                }
            } else {
                // Unified Smart Auto-Router Mode
                val sortedEndpoints = activeEndpoints.sortedByDescending { it.isPrimary }
                var success = false
                var lastError: String? = null
                
                for (i in sortedEndpoints.indices) {
                    val endpoint = sortedEndpoints[i]
                    val isLastFallback = (i == sortedEndpoints.size - 1)
                    val isSuccess = if (endpoint.type == "OLLAMA") {
                        streamOllamaModel(
                            endpoint = endpoint,
                            history = history,
                            groupId = groupId,
                            isLastFallback = isLastFallback,
                            onError = { lastError = it }
                        )
                    } else {
                        streamOpenRouterModel(
                            endpoint = endpoint,
                            history = history,
                            groupId = groupId,
                            isLastFallback = isLastFallback,
                            onError = { lastError = it }
                        )
                    }
                    if (isSuccess) {
                        success = true
                        break
                    } else {
                        android.util.Log.w("ChatViewModel", "Endpoint ${endpoint.name} failed, falling back...")
                    }
                }
                
                if (!success) {
                    _errorMessage.value = "All active endpoints failed. Last error: $lastError"
                }
            }
            
            syncMemory()
        }
    }
    
    private suspend fun streamOpenRouterModel(
        endpoint: EndpointEntity,
        history: List<OllamaMessage>,
        groupId: Long,
        isLastFallback: Boolean = true,
        onError: (String) -> Unit = {}
    ): Boolean {
        
        val mappedMessages = history.map { msg ->
            if (msg.imageUri != null) {
                val base64 = getBase64FromUri(Uri.parse(msg.imageUri))
                if (base64 != null) {
                    val parts = listOf(
                        OpenRouterContentPart(type = "text", text = msg.content),
                        OpenRouterContentPart(type = "image_url", image_url = OpenRouterImageUrl(url = base64))
                    )
                    OpenRouterMessage(role = msg.role, content = parts)
                } else {
                    OpenRouterMessage(role = msg.role, content = msg.content)
                }
            } else {
                OpenRouterMessage(role = msg.role, content = msg.content)
            }
        }
        val request = OpenRouterRequest(model = endpoint.modelName, messages = mappedMessages, stream = !endpoint.url.contains("pollinations"))

        val placeholderMsg = MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId)
        val insertedId = repository.insertMessage(placeholderMsg).toInt()
        
        withContext(Dispatchers.IO) {
            var completeResponse = ""
            try {
                var response: okhttp3.ResponseBody? = null
                var attempt = 0
                val maxRetries = 3
                while (attempt < maxRetries) {
                    try {
                        val retrofitResponse = RetrofitClient.openRouterService.generateChatStream(
                            url = endpoint.url,
                            authHeader = "Bearer ${endpoint.apiKey}",
                            request = request
                        )
                        if (!retrofitResponse.isSuccessful) {
                            throw Exception("HTTP ${retrofitResponse.code()}: ${retrofitResponse.errorBody()?.string()}")
                        }
                        response = retrofitResponse.body()
                        break
                    } catch (e: Exception) {
                        attempt++
                        android.util.Log.e("ChatViewModel", "OpenRouter connection attempt $attempt failed for ${endpoint.url}: ${e.message}")
                        if (attempt >= maxRetries) throw e
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
                if (response == null) throw Exception("Failed to connect after $maxRetries attempts")

                if (!request.stream) {
                    val fullResponse = response!!.string()
                    try {
                        val chunk = openRouterResponseAdapter.fromJson(fullResponse)
                        val choice = chunk?.choices?.firstOrNull()
                        val contentChunk = choice?.delta?.content ?: choice?.message?.content
                        if (contentChunk != null) {
                            completeResponse += contentChunk
                            ttsEngine.speak(contentChunk, flush = false)
                            repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                        }
                    } catch (e: Exception) {
                        throw Exception("Failed to parse non-streaming response: ${e.message}")
                    }
                } else {
                    val reader = BufferedReader(InputStreamReader(response!!.byteStream()))
                    
                    var ttsBuffer = ""
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
                                        ttsBuffer += contentChunk
                                        // Speak if we hit punctuation or a newline
                                        if (ttsBuffer.contains(Regex("[.!?\n]")) || (ttsBuffer.contains(" ") && ttsBuffer.length > 30)) {
                                            ttsEngine.speak(ttsBuffer, flush = false)
                                            ttsBuffer = ""
                                        }
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
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                try {
                    val embedding = embeddingEngine.generateEmbedding(completeResponse)
                    memoryDao.insert(MemoryFragment(text = completeResponse, timestamp = groupId, isUser = false, embedding = embedding.joinToString(",")))
                } catch (e: Exception) { e.printStackTrace() }
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})") }
                return@withContext true
            } catch (e: Exception) {
                if (completeResponse.isNotBlank()) {
                    android.util.Log.e("ChatViewModel", "Stream interrupted but keeping partial response: ${e.message}")
                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                    try {
                        val embedding = embeddingEngine.generateEmbedding(completeResponse)
                        memoryDao.insert(MemoryFragment(text = completeResponse, timestamp = groupId, isUser = false, embedding = embedding.joinToString(",")))
                    } catch (ex: Exception) { ex.printStackTrace() }
                    _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Working (Interrupted OK)") }
                    return@withContext true
                }

                val errorMsg = e.message ?: "Unknown Error"
                onError(errorMsg)
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: $errorMsg") }
                if (isLastFallback) {
                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: $errorMsg"))
                } else {
                    repository.deleteMessage(placeholderMsg.copy(id = insertedId))
                }
                return@withContext false
            }
        }
        return false // Fallback
    }
    
    private suspend fun streamOllamaModel(
        endpoint: EndpointEntity,
        history: List<OllamaMessage>,
        groupId: Long,
        isLastFallback: Boolean = true,
        onError: (String) -> Unit = {}
    ): Boolean {
        val request = OllamaChatRequest(model = endpoint.modelName, messages = history, stream = true)
        val placeholderMsg = MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId)
        val insertedId = repository.insertMessage(placeholderMsg).toInt()
        
        withContext(Dispatchers.IO) {
            var completeResponse = ""
            try {
                var response: okhttp3.ResponseBody? = null
                var attempt = 0
                val maxRetries = 3
                while (attempt < maxRetries) {
                    try {
                        val retrofitResponse = RetrofitClient.service.generateChatStream(endpoint.url, request)
                        if (!retrofitResponse.isSuccessful) {
                            throw Exception("HTTP ${retrofitResponse.code()}: ${retrofitResponse.errorBody()?.string()}")
                        }
                        response = retrofitResponse.body()
                        break
                    } catch (e: Exception) {
                        attempt++
                        android.util.Log.e("ChatViewModel", "Ollama connection attempt $attempt failed for ${endpoint.url}: ${e.message}")
                        if (attempt >= maxRetries) throw e
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
                if (response == null) throw Exception("Failed to connect after $maxRetries attempts")

                val reader = BufferedReader(InputStreamReader(response!!.byteStream()))
                
                var ttsBuffer = ""
                var lastUpdateTime = System.currentTimeMillis()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { jsonLine ->
                        try {
                            val chunk = responseAdapter.fromJson(jsonLine)
                            chunk?.message?.content?.let { contentChunk ->
                                completeResponse += contentChunk
                                ttsBuffer += contentChunk
                                if (ttsBuffer.contains(Regex("[.!?\n]")) || (ttsBuffer.contains(" ") && ttsBuffer.length > 30)) {
                                    ttsEngine.speak(ttsBuffer, flush = false)
                                    ttsBuffer = ""
                                }
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
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})") }
                return@withContext true
            } catch (e: Exception) {
                if (completeResponse.isNotBlank()) {
                    android.util.Log.e("ChatViewModel", "Stream interrupted but keeping partial response: ${e.message}")
                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                    try {
                        val embedding = embeddingEngine.generateEmbedding(completeResponse)
                        memoryDao.insert(MemoryFragment(text = completeResponse, timestamp = groupId, isUser = false, embedding = embedding.joinToString(",")))
                    } catch (ex: Exception) { ex.printStackTrace() }
                    _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Working (Interrupted OK)") }
                    return@withContext true
                }

                val errorMsg = e.message ?: "Unknown Error"
                onError(errorMsg)
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: $errorMsg") }
                var detailedErrorMsg = errorMsg
                if (endpoint.url.contains("10.0.2.2")) {
                    detailedErrorMsg += "\n\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                if (isLastFallback) {
                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $detailedErrorMsg"))
                } else {
                    repository.deleteMessage(placeholderMsg.copy(id = insertedId))
                }
                return@withContext false
            }
        }
        return false // Fallback
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
                syncMemory()
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
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text, imageUri = msg.imageUri))
            }
            
            history.add(OllamaMessage(role = "user", content = prompt))
            
            val newGroupId = System.currentTimeMillis()
            if (primary.type == "OLLAMA") {
                streamOllamaModel(endpoint = primary, history = history, groupId = newGroupId, isLastFallback = true)
            } else {
                streamOpenRouterModel(endpoint = primary, history = history, groupId = newGroupId, isLastFallback = true)
            }
            
            syncMemory()
        }
    }

    
    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            repository.clearMessages()
            repository.clearProfile()
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
    
    private fun syncMemory() {
        _isGenerating.value = false
        viewModelScope.launch {
            val msgs = messages.value
            memoryManager.saveEpisodicMemory(System.currentTimeMillis(), msgs)
            if (autoSyncGithub.value) {
                memoryManager.syncSessionToGithub(githubPat.value, System.currentTimeMillis(), msgs)
            }
        }
    }

    fun clearLocationSnapshots() {
        viewModelScope.launch {
            locationRepository.deleteSnapshots()
        }
    }
    suspend fun getPrimaryEndpointSync(): com.example.data.EndpointEntity? {
        val active = repository.getActiveEndpoints()
        return active.find { it.isPrimary } ?: active.firstOrNull()
    }
}

class ChatViewModelFactory(
    private val locationRepository: com.example.data.LocationRepository,
    private val astroRepository: com.example.data.AstroNumerologyRepository,
    private val localIntelligenceRepository: com.example.data.LocalIntelligenceRepository,

    private val repository: ChatRepository,
    val settingsRepository: SettingsRepository,
    private val memoryDao: MemoryFragmentDao,
    private val graphDao: com.example.data.GraphNodeDao,
    private val embeddingEngine: EmbeddingEngine,
    private val ttsEngine: com.example.ai.TTSEngine,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(locationRepository, astroRepository, localIntelligenceRepository, repository, settingsRepository, memoryDao, graphDao, embeddingEngine, ttsEngine, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    }
