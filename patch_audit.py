with open("app/src/main/java/com/example/ui/CapabilityAuditScreen.kt", "r") as f:
    code = f.read()

import re
code = code.replace("fun CapabilityAuditScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit\n)", "fun CapabilityAuditScreen(\n    viewModel: ChatViewModel,\n    onNavigateBack: () -> Unit,\n    onNavigateToCompetition: () -> Unit = {}\n)")
code = code.replace("Text(\"Status: ${item.state.name}\", color = stateColor, style = MaterialTheme.typography.labelMedium)", """Text("Status: ${item.state.name}", color = stateColor, style = MaterialTheme.typography.labelMedium)
                                
                                if (item.state == InventoryState.MISSING || item.state == InventoryState.EXPERIMENTAL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { onNavigateToCompetition() }) {
                                        Text("Initiate Research & Acquisition")
                                    }
                                }""")

with open("app/src/main/java/com/example/ui/CapabilityAuditScreen.kt", "w") as f:
    f.write(code)
