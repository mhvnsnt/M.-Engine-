import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# 1. Imports
imports = """import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterImageUrl
"""
content = content.replace('import com.example.data.SessionEntity\n', 'import com.example.data.SessionEntity\n' + imports)

# 2. Constructors
content = content.replace(
    'private val ttsEngine: com.example.ai.TTSEngine\n) : ViewModel()',
    'private val ttsEngine: com.example.ai.TTSEngine,\n    private val context: android.content.Context\n) : ViewModel()'
)
content = content.replace(
    'private val ttsEngine: com.example.ai.TTSEngine\n) : ViewModelProvider.Factory',
    'private val ttsEngine: com.example.ai.TTSEngine,\n    private val context: android.content.Context\n) : ViewModelProvider.Factory'
)
content = content.replace(
    'return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine) as T',
    'return ChatViewModel(repository, settingsRepository, memoryDao, embeddingEngine, ttsEngine, context) as T'
)

# 3. getBase64FromUri
base64_func = """
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
"""
content = content.replace('fun clearMemory() {', base64_func + '\n    fun clearMemory() {')

# 4. Modify sendMessage history loops
old_send_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }"""
new_send_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text, imageUri = msg.imageUri))
            }"""
content = content.replace(old_send_loop, new_send_loop)

old_send_add = 'history.add(OllamaMessage(role = "user", content = finalPrompt))'
new_send_add = 'history.add(OllamaMessage(role = "user", content = finalPrompt, imageUri = imageUri))'
content = content.replace(old_send_add, new_send_add)

# 5. Modify synthesizeCouncilOutputs history loops
old_sync_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }"""
new_sync_loop = """            currentMessages.filter { it.groupId < groupId }.forEach { msg ->
                history.add(OllamaMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text, imageUri = msg.imageUri))
            }"""
content = content.replace(old_sync_loop, new_sync_loop)

# 6. Modify streamOpenRouterModel request payload mapping
# Wait, let's grab exactly what's inside streamOpenRouterModel.
# It has `val request = OpenRouterRequest(model = endpoint.modelName, messages = history.map { com.example.network.OpenRouterMessage(it.role, it.content) }, stream = true)`

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
content = content.replace(old_req, new_req)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

