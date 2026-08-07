package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Stop
import com.example.ai.WhisperEngine
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlin.random.Random

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff

import com.example.ai.VoiceRecognizer

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MessageEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    workspaceViewModel: com.example.ui.WorkspaceViewModel? = null,
    onSettingsClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
        val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    
    var showMicDeniedDialog by remember { mutableStateOf(false) }
    var micRequestedBefore by rememberSaveable { mutableStateOf(false) }
    
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var locationRequestedBefore by rememberSaveable { mutableStateOf(false) }


    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val selectedFile by workspaceViewModel?.selectedFile?.collectAsStateWithLifecycle(initialValue = null) ?: remember { mutableStateOf(null) }

    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val voiceRecognizer = remember { VoiceRecognizer(context) }
    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
        val whisperEngine = remember { WhisperEngine(context) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
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
                    }
                    val githubPat by viewModel.githubPat.collectAsStateWithLifecycle()
                    
                    if (githubPat.isEmpty()) {
                        Button(
                            onClick = { 
                                // In a real app, this might show a dialog or navigate to Settings.
                                // Here, if the client ID isn't set, they must go to settings.
                                onSettingsClick() 
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Connect GitHub")
                        }
                    } else {
                        IconButton(onClick = { /* Connected */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "GitHub Connected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val groupedMessages = messages.groupBy { if (it.groupId == 0L) it.id.toLong() else it.groupId }
                items(groupedMessages.values.toList()) { groupMsgs ->
                    val userMsg = groupMsgs.find { it.isUser }
                    val aiMsgs = groupMsgs.filter { !it.isUser }
                    
                    if (userMsg != null) {
                        MessageBubble(message = userMsg, selectedFileName = selectedFile?.filePath, onApplyCode = if (workspaceViewModel != null && selectedFile != null) { code -> workspaceViewModel.updateFileContent(selectedFile!!.id, code) } else null)
                    }
                    
                    if (aiMsgs.isNotEmpty()) {
                        if (aiMsgs.size == 1) {
                            MessageBubble(message = aiMsgs.first(), selectedFileName = selectedFile?.filePath, onApplyCode = if (workspaceViewModel != null && selectedFile != null) { code -> workspaceViewModel.updateFileContent(selectedFile!!.id, code) } else null)
                        } else {
                            CouncilMessageGroup(
                                messages = aiMsgs,
                                isGenerating = isGenerating,
                                onSynthesize = { viewModel.synthesizeCouncilOutputs(aiMsgs) }
                            )
                        }
                    }
                }
                if (isGenerating) {
                    item {
                        TypingIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }

            if (selectedImageUri != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White) // using placeholder icon to close
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    placeholder = { Text("Talk to M. Engine...") },
                    enabled = !isGenerating
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("attach_button")
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
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
                        viewModel.sendMessage(inputText, selectedImageUri?.toString())
                        inputText = ""
                        selectedImageUri = null
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier.testTag("send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 150), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 300), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha3)))
    }
}

@Composable
fun MessageBubble(message: MessageEntity, selectedFileName: String? = null, onApplyCode: ((String) -> Unit)? = null) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            if (!message.isUser && message.responderName != null) {
                Text(
                    text = message.responderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                if (message.imageUri != null) {
                    AsyncImage(
                        model = android.net.Uri.parse(message.imageUri),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }

                MarkdownText(
                    text = message.text,
                    modifier = Modifier,
                    color = textColor
                )
                
                // Option B: Agentic File Edits
                if (message.text.contains("```") && onApplyCode != null && !message.isUser) {
                    
                    if (selectedFileName != null) {
                        Button(
                            onClick = {
                                // Extract the first code block
                                val regex = Regex("```(?:[a-zA-Z]*\\n)?([\\s\\S]*?)```")
                                val match = regex.find(message.text)
                                if (match != null) {
                                    val code = match.groupValues[1].trim()
                                    // Save it to the selected file
                                    onApplyCode(code)
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Apply to $selectedFileName")
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun CouncilMessageGroup(messages: List<MessageEntity>, isGenerating: Boolean, onSynthesize: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    ) {
        Column {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                edgePadding = 8.dp
            ) {
                messages.forEachIndexed { index, msg ->
                    val title = msg.responderName?.split("/")?.lastOrNull()?.split(":")?.firstOrNull() ?: "Model ${index + 1}"
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            val selectedMsg = messages.getOrNull(selectedTabIndex)
            if (selectedMsg != null) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectedMsg.text,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            if (!isGenerating && messages.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                TextButton(
                    onClick = onSynthesize,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Synthesize", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesize Consensus")
                }
            }
        }
    }
}
