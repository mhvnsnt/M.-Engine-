import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    code = f.read()

import_statement = "import com.example.ai.capabilities.connections.*\n"
if "import com.example.ai.capabilities.connections.*" not in code:
    code = code.replace("import androidx.lifecycle.ViewModel", import_statement + "import androidx.lifecycle.ViewModel")

manager_init = """
    val connectorManager = ConnectorManager(
        setOf(
            GitHubConnectionProvider(settingsRepository),
            FirebaseConnectionProvider(),
            OpenRouterConnectionProvider(settingsRepository),
            GitHubActionsConnectionProvider(settingsRepository)
        )
    )

"""

if "val connectorManager =" not in code:
    # insert inside ChatViewModel, after init or inside class body
    # let's find `val settingsRepository` line or just put it after `class ChatViewModel` definition block variables.
    code = code.replace("val jobManager = ", manager_init + "    val jobManager = ")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(code)
