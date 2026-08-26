import re

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

target1 = """fun SettingsScreen(viewModel: ChatViewModel, onNavigateBack: () -> Unit) {"""
replacement1 = """fun SettingsScreen(viewModel: ChatViewModel, onNavigateBack: () -> Unit, onNavigateToPrivacy: () -> Unit = {}) {"""
content = content.replace(target1, replacement1)

target2 = """            Button(
                onClick = {
                    viewModel.clearMemory()
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Wipe Memory")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wipe Memory & Patterns")
            }"""

replacement2 = """            Button(
                onClick = onNavigateToPrivacy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Privacy & Data Control")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy & Data Control")
            }"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
