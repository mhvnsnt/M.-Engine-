import re

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "r") as f:
    content = f.read()

# Add imports
imports = """
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
"""

content = content.replace("import com.google.accompanist.permissions.rememberPermissionState", 
                          "import com.google.accompanist.permissions.rememberPermissionState\n" + imports)

# Find top of ChatScreen composable
old_top = """@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLocalIntel: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }"""

new_top = """@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLocalIntel: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showMicDeniedDialog by remember { mutableStateOf(false) }
    var micRequestedBefore by rememberSaveable { mutableStateOf(false) }
    
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var locationRequestedBefore by rememberSaveable { mutableStateOf(false) }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }"""

content = content.replace(old_top, new_top)

# Find mic permission state
old_mic_perm = """    val voiceRecognizer = remember { VoiceRecognizer(context) }
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()"""

new_mic_perm = """    val voiceRecognizer = remember { VoiceRecognizer(context) }
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()"""

content = content.replace(old_mic_perm, new_mic_perm)

# Find Scaffold to add SnackbarHost and Location Action
old_scaffold = """    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M. Engine") },
                actions = {"""

new_scaffold = """    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("M. Engine") },
                actions = {
                    IconButton(onClick = {
                        val isGranted = locationPermissionState.allPermissionsGranted
                        val shouldShowRationale = locationPermissionState.shouldShowRationale
                        
                        if (!isGranted) {
                            if (shouldShowRationale) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Location enhances M. Engine context.")
                                }
                                locationPermissionState.launchMultiplePermissionRequest()
                            } else {
                                if (locationRequestedBefore) {
                                    showLocationDeniedDialog = true
                                } else {
                                    locationRequestedBefore = true
                                    locationPermissionState.launchMultiplePermissionRequest()
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Location is enabled.")
                            }
                        }
                    }) {
                        Icon(
                            if (locationPermissionState.allPermissionsGranted) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                            contentDescription = "Location Settings",
                            tint = if (locationPermissionState.allPermissionsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }"""

content = content.replace(old_scaffold, new_scaffold)


# Find Mic button onClick
old_mic_button = """                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            micPermissionState.launchPermissionRequest()
                        } else {
                            if (!isRecording) {"""

new_mic_button = """                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            if (micPermissionState.status.shouldShowRationale) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Voice input needs microphone access.")
                                }
                                micPermissionState.launchPermissionRequest()
                            } else {
                                if (micRequestedBefore) {
                                    showMicDeniedDialog = true
                                } else {
                                    micRequestedBefore = true
                                    micPermissionState.launchPermissionRequest()
                                }
                            }
                        } else {
                            if (!isRecording) {"""

content = content.replace(old_mic_button, new_mic_button)


# Add Dialogs before TopAppBar closes, or inside Scaffold content
# Wait, Scaffold content is:
#         }
#     ) { paddingValues ->
# Let's insert the dialogs there.

old_scaffold_content = """        }
    ) { paddingValues ->
        Column("""

new_scaffold_content = """        }
    ) { paddingValues ->
        
        if (showMicDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showMicDeniedDialog = false },
                title = { Text("Microphone Permission Denied") },
                text = { Text("Voice input needs microphone access. Please enable it in Settings to use this feature.") },
                confirmButton = {
                    TextButton(onClick = {
                        showMicDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMicDeniedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showLocationDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showLocationDeniedDialog = false },
                title = { Text("Location Permission Denied") },
                text = { Text("M. Engine uses location for context. Please enable it in Settings.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLocationDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationDeniedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        Column("""

content = content.replace(old_scaffold_content, new_scaffold_content)

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "w") as f:
    f.write(content)

