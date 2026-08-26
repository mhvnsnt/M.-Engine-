import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add statuses parameter
content = content.replace(
    "fun SettingsScreen(viewModel: ChatViewModel, onNavigateBack: () -> Unit) {",
    "fun SettingsScreen(viewModel: ChatViewModel, onNavigateBack: () -> Unit) {\n    val endpointStatuses by viewModel.endpointStatuses.collectAsStateWithLifecycle()"
)

# Display statuses
target_row = """                        Text("${endpoint.type}: ${endpoint.modelName}", style = MaterialTheme.typography.bodySmall)
                        Text(endpoint.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)"""

replacement_row = """                        Text("${endpoint.type}: ${endpoint.modelName}", style = MaterialTheme.typography.bodySmall)
                        Text(endpoint.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        val isFree = endpoint.modelName.contains("free", ignoreCase = true) || endpoint.type == "OLLAMA"
                        val costBadge = if (isFree) "Free/Local" else "Paid API"
                        val currentStatus = endpointStatuses[endpoint.id] ?: "Unknown (Not tested yet)"
                        val statusColor = when {
                            currentStatus.startsWith("Working") -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            currentStatus.startsWith("Error") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.SuggestionChip(
                                onClick = {},
                                label = { Text(costBadge) },
                                modifier = Modifier.height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currentStatus, style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }"""
content = content.replace(target_row, replacement_row)

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
