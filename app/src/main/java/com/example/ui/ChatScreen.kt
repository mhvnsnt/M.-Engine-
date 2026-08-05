package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import com.example.ai.WhisperEngine
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.random.Random

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MessageEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSettingsClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val whisperEngine = remember { WhisperEngine(context) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M. Engine") },
                actions = {
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
                        MessageBubble(message = userMsg)
                    }
                    
                    if (aiMsgs.isNotEmpty()) {
                        if (aiMsgs.size == 1) {
                            MessageBubble(message = aiMsgs.first())
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
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
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
                        viewModel.sendMessage(inputText)
                        inputText = ""
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
fun MessageBubble(message: MessageEntity) {
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
                Text(
                    text = message.text,
                    color = textColor
                )
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
