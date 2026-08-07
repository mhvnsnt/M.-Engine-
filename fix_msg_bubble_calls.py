with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

old1 = 'MessageBubble(message = userMsg)'
new1 = 'MessageBubble(message = userMsg, selectedFileName = selectedFile?.name, onApplyCode = if (workspaceViewModel != null && selectedFile != null) { code -> workspaceViewModel.updateFileContent(selectedFile!!.id, code) } else null)'

old2 = 'MessageBubble(message = aiMsgs.first())'
new2 = 'MessageBubble(message = aiMsgs.first(), selectedFileName = selectedFile?.name, onApplyCode = if (workspaceViewModel != null && selectedFile != null) { code -> workspaceViewModel.updateFileContent(selectedFile!!.id, code) } else null)'

content = content.replace(old1, new1)
content = content.replace(old2, new2)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
