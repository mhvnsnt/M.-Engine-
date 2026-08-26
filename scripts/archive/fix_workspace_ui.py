import re
with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Enhance CodeEditor
editor_old = """@Composable
fun CodeEditor(
    file: com.example.data.FileEntity,
    onContentChange: (String) -> Unit
) {
    // Basic TextField for code editing with monospace font
    TextField(
        value = file.content,
        onValueChange = onContentChange,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}"""

editor_new = """@Composable
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
                fontSize = androidx.compose.ui.unit.sp(14),
                lineHeight = androidx.compose.ui.unit.sp(20),
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}"""
content = content.replace(editor_old, editor_new)


# Enhance FileTreeViewer
filetree_old = """        items(files) { file ->
            val isSelected = selectedFile?.id == file.id
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFileSelected(file) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder, 
                    contentDescription = "File", 
                    modifier = Modifier.size(16.dp), 
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.filePath, 
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }"""
        
filetree_new = """        items(files) { file ->
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
                    imageVector = if (isFolder) Icons.Default.Folder else androidx.compose.material.icons.outlined.StarBorder, 
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
        }"""
        
content = content.replace(filetree_old, filetree_new)

# Workspace layout
layout_old = """                        items(workspaces) { workspace ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                     selectedWorkspace = workspace
                                     selectedFile = null
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
                        }"""

layout_new = """                        items(workspaces) { workspace ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                     selectedWorkspace = workspace
                                     selectedFile = null
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = if (selectedWorkspace?.id == workspace.id) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                contentColor = if (selectedWorkspace?.id == workspace.id) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(workspace.name, style = MaterialTheme.typography.titleSmall)
                                        Text(workspace.githubRepoUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f))
                                    }
                                    IconButton(onClick = { viewModel.syncWorkspace(workspace.id, workspace.githubRepoUrl) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }"""
content = content.replace(layout_old, layout_new)

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)

