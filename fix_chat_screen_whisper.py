import re
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import com.example.ai.WhisperEngine
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.random.Random
"""

content = content.replace('import androidx.compose.material.icons.filled.Star', 'import androidx.compose.material.icons.filled.Star\n' + imports)

setup = """    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val whisperEngine = remember { WhisperEngine(context) }
    val listState = rememberLazyListState()"""

content = content.replace('    var inputText by remember { mutableStateOf("") }\n    val listState = rememberLazyListState()', setup)

mic_ui = """                IconButton(
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
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton("""

content = content.replace('                IconButton(\n                    onClick = {', mic_ui + '\n                    onClick = {')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
