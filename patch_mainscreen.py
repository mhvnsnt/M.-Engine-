with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("object Connections : Screen(\"connections\", \"Connections\", Icons.Default.Settings)", "object Connections : Screen(\"connections\", \"Connections\", Icons.Default.Settings)\n    object CapabilityAudit : Screen(\"capability_audit\", \"Audit\", Icons.Default.Settings)")

code = code.replace("""composable(Screen.Connections.route) {
                ConnectionsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }""", """composable(Screen.Connections.route) {
                ConnectionsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToAudit = { navController.navigate(Screen.CapabilityAudit.route) })
            }
            composable(Screen.CapabilityAudit.route) {
                CapabilityAuditScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }""")

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(code)
