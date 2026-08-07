import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# Modify sendMessage
# currentMessages.forEach { msg ->
#     history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
# }
old_loop = """            currentMessages.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }"""
new_loop = """            currentMessages.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text, imageUri = msg.imageUri))
            }"""
content = content.replace(old_loop, new_loop)

old_add = 'history.add(OllamaMessage(role = "user", content = text))'
new_add = 'history.add(OllamaMessage(role = "user", content = text, imageUri = imageUri))'
content = content.replace(old_add, new_add)

# In synthesizeCouncilOutputs
# currentMessages.filter { it.groupId < groupId }.forEach { msg ->
#     history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
# }
old_sync_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }"""
new_sync_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text, imageUri = msg.imageUri))
            }"""
content = content.replace(old_sync_loop, new_sync_loop)

# Modify streamOpenRouterModel
start_sorm = content.find('private suspend fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long) {')
end_sorm = content.find('private suspend fun streamOllamaModel', start_sorm)

sorm = content[start_sorm:end_sorm]
old_req = 'val request = OpenRouterRequest(model = endpoint.modelName, messages = history.map { com.example.network.OpenRouterMessage(it.role, it.content) }, stream = true)'

new_req = """
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
        val request = OpenRouterRequest(model = endpoint.modelName, messages = mappedMessages, stream = true)
"""
sorm = sorm.replace(old_req, new_req)

content = content[:start_sorm] + sorm + content[end_sorm:]

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

