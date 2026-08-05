import re
with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

imports = """import com.google.firebase.auth.FirebaseAuth
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

content = content.replace('import androidx.compose.ui.text.font.FontFamily', 'import androidx.compose.ui.text.font.FontFamily\n' + imports)

auth_ui = """
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            
            Button(
                onClick = { 
                    val provider = OAuthProvider.newBuilder("github.com")
                    provider.addCustomParameter("login", "")
                    val auth = FirebaseAuth.getInstance()
                    val activity = context as Activity
                    
                    auth.startActivityForSignInWithProvider(activity, provider.build())
                        .addOnSuccessListener { authResult ->
                            val credential = authResult.credential as? com.google.firebase.auth.OAuthCredential
                            credential?.accessToken?.let { token ->
                                viewModel.updateGithubPat(token)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseAuth", "GitHub Sign-In failed", e)
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("One-Tap GitHub Sign-In (Firebase)")
            }
            
            Button(
                onClick = { 
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("YOUR_WEB_CLIENT_ID_HERE") // Replace with actual Web Client ID
                                .setAutoSelectEnabled(true)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context as Activity, request)
                            val credential = result.credential
                            if (credential is CustomCredential && credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                                    .addOnSuccessListener {
                                        // Handle success
                                    }
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseAuth", "Google One-Tap failed", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("One-Tap Google Sign-In (Credential Manager)")
            }
"""

content = content.replace('            Text("GitHub Integration", style = MaterialTheme.typography.titleMedium)', '            Text("GitHub Integration", style = MaterialTheme.typography.titleMedium)\n' + auth_ui)

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)

