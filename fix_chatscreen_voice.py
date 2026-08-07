import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add accompanist imports if not present
imports = """import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.ai.VoiceRecognizer
"""

if 'import com.example.ai.VoiceRecognizer' not in content:
    content = content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\n' + imports)

# We need to add the @OptIn(ExperimentalPermissionsApi::class) to ChatScreen if using accompanist
# Wait, let's just use it safely.
opt_in = "@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)\n@Composable"
content = content.replace("@Composable\nfun ChatScreen", opt_in + "\nfun ChatScreen")

setup = """    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val voiceRecognizer = remember { VoiceRecognizer(context) }
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
"""

if 'val micPermissionState' not in content:
    content = content.replace('val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()', setup)

# Update the Mic Icon logic
old_mic_logic = """IconButton(
                    onClick = {
                        if (!isRecording) {
                            isRecording = true
                        } else {
                            isRecording = false
                            // Simulate transcription
                            coroutineScope.launch {
                                // Simulate audio data
                                val dummyData = FloatArray(16000) { Random.nextFloat() }
                                val result = whisperEngine.transcribeAudio(dummyData)
                                inputText += if (inputText.isNotEmpty()) " $result" else result
                            }
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("mic_button")
                )"""

new_mic_logic = """IconButton(
                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            micPermissionState.launchPermissionRequest()
                        } else {
                            if (!isRecording) {
                                isRecording = true
                                voiceRecognizer.startListening(
                                    onResult = { result ->
                                        inputText = if (inputText.isNotEmpty()) "$inputText $result" else result
                                        isRecording = false
                                    },
                                    onError = { error ->
                                        // could show toast or error msg
                                        isRecording = false
                                    },
                                    onEndOfSpeech = {
                                        isRecording = false
                                    }
                                )
                            } else {
                                voiceRecognizer.stopListening()
                                isRecording = false
                            }
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("mic_button")
                )"""
                
content = content.replace(old_mic_logic, new_mic_logic)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

