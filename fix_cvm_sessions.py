import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

imports = """import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.data.SessionEntity
"""

if 'import com.example.data.SessionEntity' not in content:
    content = content.replace('import com.example.data.MessageEntity\n', 'import com.example.data.MessageEntity\n' + imports)

# Add current session state
session_state = """    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId

    val allSessions: StateFlow<List<SessionEntity>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionMessages: StateFlow<List<MessageEntity>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.getMessagesForSession(sessionId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun createNewSession() {
        viewModelScope.launch {
            val id = repository.insertSession(SessionEntity(title = "New Conversation"))
            _currentSessionId.value = id
        }
    }
"""

if 'val allSessions:' not in content:
    content = content.replace(
        'val messages: StateFlow<List<MessageEntity>> = repository.allMessages.stateIn(',
        session_state + '\n    val messages: StateFlow<List<MessageEntity>> = repository.allMessages.stateIn('
    )

# Fix sendMessage to use currentSessionId
content = content.replace('fun sendMessage(text: String, imageUri: String? = null) {', 'fun sendMessage(text: String, imageUri: String? = null) {\n        val sessionId = _currentSessionId.value ?: return')
content = content.replace('val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri)', 'val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri, sessionId = sessionId)')

# Fix stream functions to use sessionId
content = content.replace('fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long)', 'fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long, sessionId: Long)')
content = content.replace('MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId)', 'MessageEntity(text = "", isUser = false, responderName = endpoint.name, groupId = groupId, sessionId = sessionId)')

content = content.replace('fun streamOllamaModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long)', 'fun streamOllamaModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long, sessionId: Long)')

# Update synthesizeCouncilOutputs
content = content.replace('if (primary.type == "OLLAMA") {\n                streamOllamaModel(primary, history, newGroupId)\n            } else {\n                streamOpenRouterModel(primary, history, newGroupId)\n            }', 
                          'val sessionId = _currentSessionId.value ?: 1L\n            if (primary.type == "OLLAMA") {\n                streamOllamaModel(primary, history, newGroupId, sessionId)\n            } else {\n                streamOpenRouterModel(primary, history, newGroupId, sessionId)\n            }')

# And change repository.allMessages to repository.getMessagesForSession in sendMessage
content = content.replace('val currentMessages = repository.allMessages.stateIn(viewModelScope).value', 'val currentMessages = repository.getMessagesForSession(sessionId).stateIn(viewModelScope).value ?: emptyList()')

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

