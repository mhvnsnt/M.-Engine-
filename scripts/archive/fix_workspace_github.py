import re
with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.material.icons.filled.AccountCircle
"""
content = content.replace('import androidx.compose.material.icons.filled.Folder', 'import androidx.compose.material.icons.filled.Folder\n' + imports)

topbar = """        topBar = {
            TopAppBar(
                title = { Text("Workspaces") },
                actions = {
                    val settingsRepo = remember { com.example.data.SettingsRepository(androidx.compose.ui.platform.LocalContext.current) }
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
        },"""

content = content.replace("""        topBar = {
            TopAppBar(
                title = { Text("Workspaces") },
            )
        },""", topbar)

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
