import re

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add a button in SettingsScreen
button_old = 'OutlinedTextField(\n                    value = systemInstruction,\n                    onValueChange = { viewModel.updateSystemInstruction(it) },\n                    label = { Text("System Instruction / Master Prompt") },\n                    modifier = Modifier.fillMaxWidth().height(150.dp)\n                )'
button_new = """OutlinedTextField(
                    value = systemInstruction,
                    onValueChange = { viewModel.updateSystemInstruction(it) },
                    label = { Text("System Instruction / Master Prompt") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                
                Button(
                    onClick = {
                        val claudeStylePrompt = \"\"\"You are an advanced AI assistant powered by Claude Code architecture.
Your behavior is governed by the following core instructions:
- Use structured thinking inside <thinking></thinking> tags before responding.
- Formulate step-by-step plans before writing code.
- Act autonomously and confidently.
- Always include the user's workspace context and system constraints in your evaluation.
- Speak with the precision, depth, and clarity characteristic of Anthropic's Opus and Fable models.\"\"\"
                        viewModel.updateSystemInstruction(claudeStylePrompt)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import 'Claude Code' Open-Source Persona")
                }"""

content = content.replace(button_old, button_new)

with open('app/src/main/java/com/example/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)

