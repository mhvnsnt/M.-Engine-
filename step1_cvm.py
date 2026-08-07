import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# 1. Imports
imports = """
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterImageUrl
"""
if 'import android.net.Uri' not in content:
    content = content.replace('import androidx.lifecycle.ViewModel\n', 'import androidx.lifecycle.ViewModel\n' + imports)

# 2. Context constructor
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

# 3. getBase64FromUri method
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
if 'private fun getBase64FromUri' not in content:
    content = content.replace('fun clearMemory() {', base64_func + '\n    fun clearMemory() {')

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)

