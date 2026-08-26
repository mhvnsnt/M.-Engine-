import re

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

target1 = """    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}"""

replacement1 = """    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Privacy : Screen("privacy", "Privacy", Icons.Default.Settings) // Not in bottom bar
}"""
content = content.replace(target1, replacement1)

target2 = """            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }"""

replacement2 = """            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) })
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)
