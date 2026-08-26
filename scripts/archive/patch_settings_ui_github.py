import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

if "autoSyncGithub" not in content:
    content = content.replace("val initialGithubPat by viewModel.githubPat.collectAsStateWithLifecycle()", "val autoSyncGithub by viewModel.autoSyncGithub.collectAsStateWithLifecycle()\n    val pullMemoryOnStart by viewModel.pullMemoryOnStart.collectAsStateWithLifecycle()\n    val initialGithubPat by viewModel.githubPat.collectAsStateWithLifecycle()")

    ui_code = """
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-sync conversations to GitHub", modifier = Modifier.weight(1f))
                    Switch(
                        checked = autoSyncGithub,
                        onCheckedChange = { viewModel.updateAutoSyncGithub(it) }
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pull memory on start", modifier = Modifier.weight(1f))
                    Switch(
                        checked = pullMemoryOnStart,
                        onCheckedChange = { viewModel.updatePullMemoryOnStart(it) }
                    )
                }
"""
    content = content.replace('onDismiss = { viewModel.cancelGithubDeviceFlow() }\n                )', 'onDismiss = { viewModel.cancelGithubDeviceFlow() }\n                )\n' + ui_code)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)

