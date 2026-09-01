with open('app/src/main/java/com/example/ui/ConnectionsScreen.kt', 'r') as f:
    content = f.read()

import re
old_audit = '''                    onClick = { onNavigateToAudit() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Capabilities")
                }
            }'''
new_audit = '''                    onClick = { onNavigateToAudit() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Capabilities")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onNavigateToFabric() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Manage Execution Fabric")
                }
            }'''
content = content.replace(old_audit, new_audit)
content = content.replace('fun ConnectionsScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit,\n    onNavigateToAudit: () -> Unit = {}', 'fun ConnectionsScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit,\n    onNavigateToAudit: () -> Unit = {},\n    onNavigateToFabric: () -> Unit = {}')

with open('app/src/main/java/com/example/ui/ConnectionsScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    main_content = f.read()

main_content = main_content.replace('''            ConnectionsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.CHAT },
                onNavigateToAudit = { currentScreen = Screen.AUDIT }
            )''', '''            ConnectionsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.CHAT },
                onNavigateToAudit = { currentScreen = Screen.AUDIT },
                onNavigateToFabric = { currentScreen = Screen.FABRIC }
            )''')
with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
    f.write(main_content)

