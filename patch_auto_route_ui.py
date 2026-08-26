import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

target = "    val pullMemoryOnStart by viewModel.pullMemoryOnStart.collectAsStateWithLifecycle()"
new_target = "    val pullMemoryOnStart by viewModel.pullMemoryOnStart.collectAsStateWithLifecycle()\n    val councilMode by viewModel.councilMode.collectAsStateWithLifecycle()"
content = content.replace(target, new_target)

ui_target = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pull Memory On Start", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = pullMemoryOnStart, onCheckedChange = { viewModel.updatePullMemoryOnStart(it) })
                }"""

new_ui = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pull Memory On Start", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = pullMemoryOnStart, onCheckedChange = { viewModel.updatePullMemoryOnStart(it) })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Council Mode (Query All)", style = MaterialTheme.typography.bodyLarge)
                        Text("If disabled, uses Smart Auto-Router (tries Primary first, falls back to others on error).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = councilMode, onCheckedChange = { viewModel.updateCouncilMode(it) })
                }"""
content = content.replace(ui_target, new_ui)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
