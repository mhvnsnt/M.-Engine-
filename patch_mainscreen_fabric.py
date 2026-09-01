with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

import re

# Add Fabric to Screen Enum
old_enum = '''enum class Screen {
    CHAT, WORKSPACE, CONNECTIONS, CAPABILITY_COMPETITION, SETTINGS, PRIVACY, OBSERVATORY, AUDIT, EVIDENCE
}'''
new_enum = '''enum class Screen {
    CHAT, WORKSPACE, CONNECTIONS, CAPABILITY_COMPETITION, SETTINGS, PRIVACY, OBSERVATORY, AUDIT, EVIDENCE, FABRIC
}'''
content = content.replace(old_enum, new_enum)

# Add Fabric route
old_route = '''        Screen.EVIDENCE -> {
            EvidenceScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.OBSERVATORY }
            )
        }'''
new_route = '''        Screen.EVIDENCE -> {
            EvidenceScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.OBSERVATORY }
            )
        }
        Screen.FABRIC -> {
            FabricScreen(
                onNavigateBack = { currentScreen = Screen.CONNECTIONS }
            )
        }'''
content = content.replace(old_route, new_route)

# Add Fabric to menu or connections screen? Let's add it to Settings menu or Workspace menu.
# Wait, let's just make sure it's accessible. In WorkspaceScreen or SettingsScreen?
with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(content)

