package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.DeviceFlowState
import com.example.data.EndpointEntity

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val styleProfile by viewModel.styleProfile.collectAsStateWithLifecycle()
    val initialSystemInstruction by viewModel.systemInstruction.collectAsStateWithLifecycle()
    val endpoints by viewModel.endpoints.collectAsStateWithLifecycle()
    val initialGithubClientId by viewModel.githubClientId.collectAsStateWithLifecycle()
    var githubClientId by remember { mutableStateOf(initialGithubClientId) }
    val initialUseWhisper by viewModel.useWhisperModel.collectAsStateWithLifecycle()
    var useWhisper by remember { mutableStateOf(initialUseWhisper) }
    
    val initialVoiceAdaptation by viewModel.voiceAdaptation.collectAsStateWithLifecycle()
    var voiceAdaptation by remember { mutableStateOf(initialVoiceAdaptation) }
    
    val initialTranscriptionLang by viewModel.transcriptionLanguage.collectAsStateWithLifecycle()
    var transcriptionLang by remember { mutableStateOf(initialTranscriptionLang) }

    LaunchedEffect(initialUseWhisper) { useWhisper = initialUseWhisper }
    LaunchedEffect(initialVoiceAdaptation) { voiceAdaptation = initialVoiceAdaptation }
    LaunchedEffect(initialTranscriptionLang) { transcriptionLang = initialTranscriptionLang }

    val deviceFlowState by viewModel.deviceFlowState.collectAsStateWithLifecycle()

    LaunchedEffect(initialGithubClientId) {
        if (githubClientId != initialGithubClientId) {
            githubClientId = initialGithubClientId
        }
    }
    val initialGithubPat by viewModel.githubPat.collectAsStateWithLifecycle()
    
    var systemInstruction by remember { mutableStateOf(initialSystemInstruction) }
    var githubPat by remember { mutableStateOf(initialGithubPat) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(initialSystemInstruction) {
        if (systemInstruction != initialSystemInstruction) {
            systemInstruction = initialSystemInstruction
        }
    }
    
    LaunchedEffect(initialGithubPat) {
        if (githubPat != initialGithubPat) {
            githubPat = initialGithubPat
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Endpoint Cart (AI Council)", style = MaterialTheme.typography.titleMedium)
            Text("Enable multiple endpoints to query them simultaneously. Star one as the Primary for synthesis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            endpoints.forEach { endpoint ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (endpoint.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(endpoint.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.setPrimaryEndpoint(endpoint) }) {
                                Icon(
                                    imageVector = if (endpoint.isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Primary",
                                    tint = if (endpoint.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = endpoint.isActive,
                                onCheckedChange = { viewModel.toggleEndpointActive(endpoint, it) }
                            )
                        }
                        Text("${endpoint.type}: ${endpoint.modelName}", style = MaterialTheme.typography.bodySmall)
                        Text(endpoint.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.deleteEndpoint(endpoint) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Endpoint")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Endpoint")
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Persona Configuration", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = systemInstruction,
                onValueChange = { 
                    systemInstruction = it
                    viewModel.updateSystemInstruction(it) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("system_instruction_input"),
                label = { Text("System Instruction") }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            

            Text("GitHub Integration", style = MaterialTheme.typography.titleMedium)

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

            
            OutlinedTextField(
                value = githubClientId,
                onValueChange = {
                    githubClientId = it
                    viewModel.updateGithubClientId(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GitHub OAuth Client ID") },
                placeholder = { Text("Ov23...") }
            )
            
            Button(
                onClick = { viewModel.startGithubDeviceFlow(githubClientId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = githubClientId.isNotBlank() && deviceFlowState == null
            ) {
                Text("Connect with GitHub (Device Flow)")
            }
            
            OutlinedTextField(
                value = githubPat,
                onValueChange = {
                    githubPat = it
                    viewModel.updateGithubPat(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GitHub Personal Access Token (PAT) / Auth Token") },
                placeholder = { Text("ghp_...") }
            )
            
            if (deviceFlowState != null) {
                DeviceFlowDialog(
                    state = deviceFlowState!!,
                    onDismiss = { viewModel.cancelGithubDeviceFlow() }
                )
            }

            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Voice Transcription Settings", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use Whisper Model", style = MaterialTheme.typography.bodyMedium)
                    Text("Local, high-fidelity ONNX model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = useWhisper,
                    onCheckedChange = { 
                        useWhisper = it
                        viewModel.updateUseWhisperModel(it)
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice Adaptation", style = MaterialTheme.typography.bodyMedium)
                    Text("Learn and adapt to speech patterns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = voiceAdaptation,
                    onCheckedChange = { 
                        voiceAdaptation = it
                        viewModel.updateVoiceAdaptation(it)
                    }
                )
            }
            
            OutlinedTextField(
                value = transcriptionLang,
                onValueChange = {
                    transcriptionLang = it
                    viewModel.updateTranscriptionLanguage(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Transcription Language Code") },
                placeholder = { Text("en, es, fr...") }
            )

            Text("Local Memory & Patterns", style = MaterialTheme.typography.titleMedium)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val msgs = styleProfile?.totalMessages ?: 0
                    val avgLength = if (msgs > 0) (styleProfile?.totalWords ?: 0) / msgs else 0
                    val topics = styleProfile?.topics?.takeIf { it.isNotBlank() } ?: "None"
                    
                    Text("Interactions Analyzed: $msgs")
                    Text("Avg Sentence Length: $avgLength words")
                    Text("Learned Topics: $topics")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.clearMemory()
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Wipe Memory")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wipe Memory & Patterns")
            }
        }
        
        if (showAddDialog) {
            AddEndpointDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, url, apiKey, modelName, type ->
                    viewModel.addEndpoint(name, url, apiKey, modelName, type)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddEndpointDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var isOllama by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Endpoint") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type:")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = isOllama, onClick = { isOllama = true }, label = { Text("Ollama") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = !isOllama, onClick = { isOllama = false }, label = { Text("OpenAI / OpenRouter") })
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, placeholder = { Text(if (isOllama) "Local Ollama" else "Groq") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, placeholder = { Text(if (isOllama) "http://10.0.2.2:11434/api/chat" else "https://api.groq.com/openai/v1/chat/completions") })
                OutlinedTextField(value = modelName, onValueChange = { modelName = it }, label = { Text("Model ID") }, placeholder = { Text(if (isOllama) "gemma:2b" else "llama3-8b-8192") })
                if (!isOllama) {
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, placeholder = { Text("sk-...") })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, url, apiKey, modelName, if (isOllama) "OLLAMA" else "OPENAI") },
                enabled = name.isNotBlank() && url.isNotBlank() && modelName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun DeviceFlowDialog(state: DeviceFlowState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Device Authorization") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                } else if (state.userCode.isNotBlank()) {
                    Text("Please enter this code on GitHub:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = state.userCode,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { uriHandler.openUri(state.verificationUri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open GitHub to Authorize")
                    }
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Waiting for authorization...", style = MaterialTheme.typography.bodySmall)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Requesting device code...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
