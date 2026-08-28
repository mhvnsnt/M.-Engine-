with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("object CapabilityAudit : Screen(\"capability_audit\", \"Audit\", Icons.Default.Settings)", "object CapabilityAudit : Screen(\"capability_audit\", \"Audit\", Icons.Default.Settings)\n    object CapabilityCompetition : Screen(\"capability_competition\", \"Competition\", Icons.Default.Settings)")

code = code.replace("""composable(Screen.CapabilityAudit.route) {
                CapabilityAuditScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }""", """composable(Screen.CapabilityAudit.route) {
                CapabilityAuditScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToCompetition = { navController.navigate(Screen.CapabilityCompetition.route) })
            }
            composable(Screen.CapabilityCompetition.route) {
                CapabilityCompetitionScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }""")

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(code)
