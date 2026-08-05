import re
with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

state_vars = """
    val initialUseWhisper by viewModel.useWhisperModel.collectAsStateWithLifecycle()
    var useWhisper by remember { mutableStateOf(initialUseWhisper) }
    
    val initialVoiceAdaptation by viewModel.voiceAdaptation.collectAsStateWithLifecycle()
    var voiceAdaptation by remember { mutableStateOf(initialVoiceAdaptation) }
    
    val initialTranscriptionLang by viewModel.transcriptionLanguage.collectAsStateWithLifecycle()
    var transcriptionLang by remember { mutableStateOf(initialTranscriptionLang) }

    LaunchedEffect(initialUseWhisper) { useWhisper = initialUseWhisper }
    LaunchedEffect(initialVoiceAdaptation) { voiceAdaptation = initialVoiceAdaptation }
    LaunchedEffect(initialTranscriptionLang) { transcriptionLang = initialTranscriptionLang }
"""

content = content.replace('    var githubClientId by remember { mutableStateOf(initialGithubClientId) }', '    var githubClientId by remember { mutableStateOf(initialGithubClientId) }' + state_vars)

ui_code = """
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
"""

content = content.replace('            Text("Local Memory & Patterns", style = MaterialTheme.typography.titleMedium)', ui_code + '\n            Text("Local Memory & Patterns", style = MaterialTheme.typography.titleMedium)')

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
