import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

target1 = "    val initialGithubClientId by viewModel.githubClientId.collectAsStateWithLifecycle()"
new1 = """    val initialTelegramToken by viewModel.telegramBotToken.collectAsStateWithLifecycle()
    var telegramToken by remember { mutableStateOf(initialTelegramToken) }
    LaunchedEffect(initialTelegramToken) { telegramToken = initialTelegramToken }
    
    val initialGithubClientId by viewModel.githubClientId.collectAsStateWithLifecycle()"""
content = content.replace(target1, new1)

target2 = """                OutlinedTextField(
                    value = openRouterKey,"""
new2 = """                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { telegramToken = it },
                    label = { Text("Telegram Bot Token (Headless AI Clone)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.updateTelegramBotToken(telegramToken) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Telegram Token")
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = openRouterKey,"""
content = content.replace(target2, new2)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
