with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("connectorManager.discoverAll()", "connectorManager.checkAllHealth()")
code = code.replace("connectorManager.authorizeProvider(provider.id, context as? Activity)", "connectorManager.connectProvider(provider.id, context as? Activity)")
code = code.replace("connectorManager.revokeProvider(provider.id)", "connectorManager.disconnectProvider(provider.id)")

# Fix the ConnectionRow signature and usage
code = code.replace("provider: ConnectionProvider,", "provider: ConnectionProvider,\n    capabilities: Set<CapabilityType>,")
code = code.replace("provider.capabilities.joinToString", "capabilities.joinToString")

code = code.replace("""
                val status = connectionStates[provider.id] ?: ConnectionStatus.UNCONFIGURED
                ConnectionRow(
                    provider = provider,
                    status = status,
""", """
                val status = connectionStates[provider.id] ?: ConnectionStatus.UNCONFIGURED
                var caps by remember { mutableStateOf<Set<CapabilityType>>(emptySet()) }
                LaunchedEffect(provider) {
                    caps = provider.discoverCapabilities()
                }
                ConnectionRow(
                    provider = provider,
                    capabilities = caps,
                    status = status,
""")

with open("app/src/main/java/com/example/ui/ConnectionsScreen.kt", "w") as f:
    f.write(code)
