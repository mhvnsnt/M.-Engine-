with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'r') as f:
    content = f.read()

# Add to the top of ChatScreen:
top_code = """
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val selectedFile by workspaceViewModel?.selectedFile?.collectAsStateWithLifecycle(initialValue = null) ?: remember { mutableStateOf(null) }
"""

content = content.replace('    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }', top_code)

# Remove the one inside the block:
content = content.replace('val selectedFile by workspaceViewModel.selectedFile.collectAsStateWithLifecycle()', '')

with open('app/src/main/java/com/example/ui/ChatScreen.kt', 'w') as f:
    f.write(content)
