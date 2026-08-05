import re
with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

imports = """
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
"""

content = content.replace('import com.example.data.EndpointEntity', 'import com.example.data.EndpointEntity\n' + imports)

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
