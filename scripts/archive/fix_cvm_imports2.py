import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

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

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
