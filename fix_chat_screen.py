import re

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add a WorkspaceViewModel parameter to ChatScreen
content = content.replace(
    'fun ChatScreen(\n    viewModel: ChatViewModel,\n    onSettingsClick: () -> Unit\n) {',
    'fun ChatScreen(\n    viewModel: ChatViewModel,\n    workspaceViewModel: com.example.ui.WorkspaceViewModel? = null,\n    onSettingsClick: () -> Unit\n) {'
)

# Extract code block logic
button_code = """
                MarkdownText(
                    text = message.text,
                    modifier = Modifier,
                    color = textColor
                )
                
                // Option B: Agentic File Edits
                if (message.text.contains("```") && workspaceViewModel != null && !message.isUser) {
                    val selectedFile by workspaceViewModel.selectedFile.collectAsStateWithLifecycle()
                    if (selectedFile != null) {
                        Button(
                            onClick = {
                                // Extract the first code block
                                val regex = Regex("```(?:[a-zA-Z]*\\\\n)?([\\\\s\\\\S]*?)```")
                                val match = regex.find(message.text)
                                if (match != null) {
                                    val code = match.groupValues[1].trim()
                                    // Save it to the selected file
                                    workspaceViewModel.saveFile(code)
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Apply to \${selectedFile?.name}")
                        }
                    }
                }
"""

content = content.replace(
    '                MarkdownText(\n                    text = message.text,\n                    modifier = Modifier,\n                    color = textColor\n                )',
    button_code
)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
