import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Fix OpenRouter mismatch:
# In ChatViewModel: fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long, sessionId: Long)
# It should convert history to List<OpenRouterMessage>
content = content.replace(
    'val request = OpenRouterRequest(model = endpoint.modelName, messages = history, stream = true)',
    'val request = OpenRouterRequest(model = endpoint.modelName, messages = history.map { com.example.network.OpenRouterMessage(it.role, it.content) }, stream = true)'
)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

