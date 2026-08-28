with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("import com.example.ai.capabilities.connections.ConnectionStatus", "import com.example.ai.capabilities.connections.ConnectionStatus\nimport com.example.ai.capabilities.connections.CapabilityType\nimport com.example.ai.capabilities.connections.RealityClassification")

code = code.replace("""                ConnectionStatus.BLOCKED_BY_EXTERNAL_DEPENDENCY -> {
                    Text("Blocked", color = MaterialTheme.colorScheme.error)
                }""", """                ConnectionStatus.PENDING_AUTHORIZATION -> {
                    Button(onClick = onConnect) {
                        Text("Resume Auth")
                    }
                }""")

with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "w") as f:
    f.write(code)
