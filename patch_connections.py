with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("fun ConnectionsScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit\n)", "fun ConnectionsScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit,\n    onNavigateToAudit: () -> Unit = {}\n)")
code = code.replace("onClick = { /* Navigate to manage capabilities */ }", "onClick = { onNavigateToAudit() }")

with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "w") as f:
    f.write(code)
