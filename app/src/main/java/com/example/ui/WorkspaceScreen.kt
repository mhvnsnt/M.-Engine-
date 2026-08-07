package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.AccountCircle

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel
) {
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWorkspace by remember { mutableStateOf<com.example.data.WorkspaceEntity?>(null) }
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspaces") },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val settingsRepo = remember(context) { com.example.data.SettingsRepository(context) }
                    val githubPat by settingsRepo.githubPatFlow.collectAsStateWithLifecycle(initialValue = "")
                    
                    if (githubPat.isEmpty()) {
                        Button(
                            onClick = { /* Navigate to Settings or show auth dialog */ },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Connect GitHub")
                        }
                    } else {
                        IconButton(onClick = { /* Connected */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "GitHub Connected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Workspace")
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left Panel (Workspaces & File Tree)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (syncStatus.isNotBlank()) {
                    Text(syncStatus, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (workspaces.isEmpty()) {
                        item {
                            Text("No workspaces yet. Add a GitHub repository to get started.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(workspaces) { workspace ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    selectedWorkspace = workspace 
                                    viewModel.selectFile(null)
                                },
                                colors = CardDefaults.cardColors(containerColor = if (selectedWorkspace?.id == workspace.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(workspace.name, style = MaterialTheme.typography.titleMedium)
                                        Text(workspace.githubRepoUrl, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { viewModel.syncWorkspace(workspace.id, workspace.githubRepoUrl) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                                    }
                                }
                            }
                        }
                    }
                }
                
                selectedWorkspace?.let { workspace ->
                    HorizontalDivider()
                    Text("File Tree (Live Map)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    FileTreeViewer(
                        viewModel = viewModel, 
                        workspaceId = workspace.id,
                        selectedFile = selectedFile,
                        onFileSelected = { viewModel.selectFile(it) }
                    )
                }
            }
            
            // Right Panel (Code Editor)
            if (selectedFile != null) {
                VerticalDivider()
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = selectedFile!!.filePath,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()
                    CodeEditor(
                        file = selectedFile!!,
                        onContentChange = { newContent ->
                            
                            viewModel.updateFileContent(selectedFile!!.id, newContent)
                        }
                    )
                }
            }
        }
        
        if (showAddDialog) {
            AddWorkspaceDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, repoUrl ->
                    viewModel.addWorkspace(name, repoUrl)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddWorkspaceDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add GitHub Repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workspace Name") },
                    placeholder = { Text("My App Clone") }
                )
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("GitHub Repo URL") },
                    placeholder = { Text("https://github.com/user/repo") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, repoUrl) },
                enabled = name.isNotBlank() && repoUrl.isNotBlank()
            ) {
                Text("Clone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FileTreeViewer(
    viewModel: WorkspaceViewModel, 
    workspaceId: Long,
    selectedFile: com.example.data.FileEntity?,
    onFileSelected: (com.example.data.FileEntity) -> Unit
) {
    val files by viewModel.getFilesForWorkspace(workspaceId).collectAsStateWithLifecycle()
    
    if (files.isEmpty()) {
        Text("No files synced yet.", modifier = Modifier.padding(16.dp))
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(files) { file ->
            val isSelected = selectedFile?.id == file.id
            val isFolder = !file.filePath.contains(".")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFileSelected(file) }
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (isFolder) Icons.Default.Folder else Icons.Outlined.StarBorder, 
                    contentDescription = "File", 
                    modifier = Modifier.size(16.dp), 
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.filePath, 
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable

fun CodeEditor(
    file: com.example.data.FileEntity,
    onContentChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        BasicTextField(
            value = file.content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = SyntaxHighlighter()
        )
    }
}


class SyntaxHighlighter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val inputText = text.text
        val annotatedString = buildAnnotatedString {
            append(inputText)
            
            // Keywords
            val keywords = listOf("val ", "var ", "fun ", "class ", "interface ", "return ", "if ", "else ", "for ", "while ", "import ", "package ", "import\n")
            keywords.forEach { keyword ->
                var startIndex = inputText.indexOf(keyword)
                while (startIndex >= 0) {
                    addStyle(
                        style = SpanStyle(color = Color(0xFFC678DD)), // Purple
                        start = startIndex,
                        end = startIndex + keyword.length
                    )
                    startIndex = inputText.indexOf(keyword, startIndex + keyword.length)
                }
            }
            
            // Strings
            val stringRegex = "\".*?\"".toRegex()
            stringRegex.findAll(inputText).forEach { matchResult ->
                addStyle(
                    style = SpanStyle(color = Color(0xFF98C379)), // Green
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }
            
            // Comments
            val commentRegex = "//.*".toRegex()
            commentRegex.findAll(inputText).forEach { matchResult ->
                addStyle(
                    style = SpanStyle(color = Color(0xFF5C6370)), // Gray
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

