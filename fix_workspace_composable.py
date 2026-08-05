import re
with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

bad = """                    val settingsRepo = remember { com.example.data.SettingsRepository(androidx.compose.ui.platform.LocalContext.current) }
                    val githubPat by settingsRepo.githubPatFlow.collectAsStateWithLifecycle(initialValue = "")"""

good = """                    val context = androidx.compose.ui.platform.LocalContext.current
                    val settingsRepo = remember(context) { com.example.data.SettingsRepository(context) }
                    val githubPat by settingsRepo.githubPatFlow.collectAsStateWithLifecycle(initialValue = "")"""

content = content.replace(bad, good)

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
