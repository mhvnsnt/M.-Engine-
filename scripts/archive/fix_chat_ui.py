import re
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Make input box look cleaner
input_box_old = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                placeholder = { Text("Ask anything...") },
                enabled = !isGenerating
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                IconButton(
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
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier.testTag("send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }"""

input_box_new = """        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (!isRecording) {
                            isRecording = true
                        } else {
                            isRecording = false
                            coroutineScope.launch {
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
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("chat_input"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("Ask M. Engine anything...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        innerTextField()
                    },
                    enabled = !isGenerating
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier.testTag("send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }"""

content = content.replace(input_box_old, input_box_new)

# Make Empty State beautiful
empty_state_old = """            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Start a conversation!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {"""

empty_state_new = """            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "M. Engine", 
                            style = MaterialTheme.typography.displayMedium, 
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "How can I help you today?", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {"""
            
content = content.replace(empty_state_old, empty_state_new)

# Enhance Chat Bubbles
bubble_old = """                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (msg.isUser) "You" else msg.responderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }"""

bubble_new = """                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = if (msg.isUser) 
                        androidx.compose.foundation.shape.RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) 
                    else 
                        androidx.compose.foundation.shape.RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                    color = if (msg.isUser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    contentColor = if (msg.isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                ) {
                    Column(modifier = Modifier.padding(if (msg.isUser) 16.dp else 4.dp)) {
                        if (!msg.isUser) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                Icon(Icons.Default.Star, contentDescription = "AI", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.responderName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                            lineHeight = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Em)
                        )
                    }
                }"""

# ensure BasicTextField and Color are imported
imports_new = """import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
"""
content = content.replace('import androidx.compose.material.icons.filled.Stop', imports_new + 'import androidx.compose.material.icons.filled.Stop')

content = content.replace(bubble_old, bubble_new)


with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)

