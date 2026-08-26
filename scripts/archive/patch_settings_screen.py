with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

target = """fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {"""
replacement = """fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {}
) {"""
content = content.replace(target, replacement)

target2 = """                Text(
                    text = "GitHub Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )"""
replacement2 = """                Button(
                    onClick = onNavigateToPrivacy,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("Privacy & Data Dashboard")
                }
                
                Text(
                    text = "GitHub Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
