with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'ChatScreen(viewModel = viewModel, onSettingsClick = { navController.navigate(Screen.Settings.route) })',
    'ChatScreen(viewModel = viewModel, workspaceViewModel = workspaceViewModel, onSettingsClick = { navController.navigate(Screen.Settings.route) })'
)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)
