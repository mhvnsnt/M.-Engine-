import re

with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'r') as f:
    content = f.read()

if 'val selectedFile' not in content:
    content = content.replace(
        'val syncStatus: StateFlow<String> = _syncStatus',
        'val syncStatus: StateFlow<String> = _syncStatus\n    private val _selectedFile = MutableStateFlow<FileEntity?>(null)\n    val selectedFile: StateFlow<FileEntity?> = _selectedFile\n\n    fun selectFile(file: FileEntity?) {\n        _selectedFile.value = file\n    }'
    )

with open('app/src/main/java/com/example/ui/WorkspaceViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Replace local state with ViewModel state
old_state = "var selectedFile by remember { mutableStateOf<com.example.data.FileEntity?>(null) }"
new_state = "val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()"
content = content.replace(old_state, new_state)

content = content.replace('selectedFile = null', 'viewModel.selectFile(null)')
content = content.replace('selectedFile = it', 'viewModel.selectFile(it)')
content = content.replace('selectedFile = selectedFile!!.copy(content = newContent)', '') # Not needed locally if updating via viewmodel

# And update MainScreen to observe this and pass to ChatViewModel
with open('app/src/main/java/com/example/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()
    
# Import collectAsState
if 'import androidx.lifecycle.compose.collectAsStateWithLifecycle' not in content:
    content = content.replace('import androidx.compose.runtime.getValue', 'import androidx.compose.runtime.getValue\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport androidx.compose.runtime.LaunchedEffect')

# Observe and set context
effect = """@Composable
fun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {
    val selectedFile by workspaceViewModel.selectedFile.collectAsStateWithLifecycle()
    LaunchedEffect(selectedFile) {
        viewModel.workspaceContext.value = selectedFile?.content
    }
    val navController = rememberNavController()"""

content = content.replace('@Composable\nfun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {\n    val navController = rememberNavController()', effect)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)

