with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {"""
replacement = """import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {
    val isOnboardingComplete by viewModel.settingsRepository.isOnboardingCompleteFlow.collectAsStateWithLifecycle(initialValue = false)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    if (!isOnboardingComplete) {
        OnboardingScreen(onComplete = {
            coroutineScope.launch {
                viewModel.settingsRepository.completeOnboarding()
            }
        })
        return
    }
"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)
