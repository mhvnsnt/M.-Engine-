with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun MessageBubble(message: MessageEntity) {',
    'fun MessageBubble(message: MessageEntity, selectedFileName: String? = null, onApplyCode: ((String) -> Unit)? = null) {'
)

# Fix the internal variables
content = content.replace(
    'if (message.text.contains("```") && workspaceViewModel != null && !message.isUser) {',
    'if (message.text.contains("```") && onApplyCode != null && !message.isUser) {'
)
content = content.replace(
    'if (selectedFile != null) {',
    'if (selectedFileName != null) {'
)
content = content.replace(
    'workspaceViewModel.updateFileContent(selectedFile!!.id, code)',
    'onApplyCode(code)'
)
content = content.replace(
    'Text("Apply to \\${selectedFile?.name}")',
    'Text("Apply to $selectedFileName")'
)

# Fix the call sites of MessageBubble
content = content.replace(
    'MessageBubble(message = message)',
    'MessageBubble(message = message, selectedFileName = selectedFile?.name, onApplyCode = if (workspaceViewModel != null && selectedFile != null) { code -> workspaceViewModel.updateFileContent(selectedFile!!.id, code) } else null)'
)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
