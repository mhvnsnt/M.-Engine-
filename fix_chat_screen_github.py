import re
with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.material.icons.filled.AccountCircle
"""
content = content.replace('import androidx.compose.material.icons.filled.Settings', 'import androidx.compose.material.icons.filled.Settings\n' + imports)

topbar = """        topBar = {
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
        }"""

content = content.replace("""        topBar = {
            TopAppBar(
                title = { Text("M. Engine") },
                actions = {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }""", topbar)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
