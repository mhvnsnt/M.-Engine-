import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {"""
new_target = """fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val endpointStatuses by viewModel.endpointStatuses.collectAsStateWithLifecycle()"""

content = content.replace(target, new_target)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
