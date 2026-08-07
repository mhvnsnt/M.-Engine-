with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'workspaceViewModel.saveFile(code)',
    'workspaceViewModel.updateFileContent(selectedFile!!.id, code)'
)

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
