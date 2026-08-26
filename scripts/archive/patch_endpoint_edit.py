import re

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add edit dialog state
if "var showEditDialog by remember" not in content:
    content = content.replace("var showAddDialog by remember { mutableStateOf(false) }", "var showAddDialog by remember { mutableStateOf(false) }\n    var showEditDialog by remember { mutableStateOf<com.example.data.EndpointEntity?>(null) }")

# Add Edit button next to Delete button
if "Text(\"Edit\")" not in content:
    content = content.replace('TextButton(onClick = { viewModel.deleteEndpoint(endpoint) }', 'TextButton(onClick = { showEditDialog = endpoint }) {\n                                Text("Edit")\n                            }\n                            TextButton(onClick = { viewModel.deleteEndpoint(endpoint) }')

# Add EditDialog Composable
if "fun EditEndpointDialog" not in content:
    content += """
@Composable
fun EditEndpointDialog(
    endpoint: com.example.data.EndpointEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(endpoint.apiKey) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${endpoint.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(apiKey) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
"""

# Call EditDialog
if "showEditDialog?.let" not in content:
    content = content.replace('if (showAddDialog) {', 'showEditDialog?.let { endpoint ->\n            EditEndpointDialog(\n                endpoint = endpoint,\n                onDismiss = { showEditDialog = null },\n                onSave = { apiKey ->\n                    viewModel.updateEndpointApiKey(endpoint.id, apiKey)\n                    showEditDialog = null\n                }\n            )\n        }\n\n        if (showAddDialog) {')

with open("app/src/main/java/com/example/ui/SettingsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

if "fun updateEndpointApiKey" not in content:
    content = content.replace('fun deleteEndpoint(endpoint: EndpointEntity)', 'fun updateEndpointApiKey(id: Long, apiKey: String) {\n        viewModelScope.launch {\n            repository.updateEndpointApiKey(id, apiKey)\n        }\n    }\n\n    fun deleteEndpoint(endpoint: EndpointEntity)')

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/LocalIntelligenceRepository.kt", "r") as f:
    content = f.read()
    
if "fun updateEndpointApiKey" not in content:
    content = content.replace('suspend fun insertEndpoint', 'suspend fun updateEndpointApiKey(id: Long, apiKey: String) {\n        endpointDao.updateApiKey(id, apiKey)\n    }\n\n    suspend fun insertEndpoint')

with open("app/src/main/java/com/example/data/LocalIntelligenceRepository.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/EndpointDao.kt", "r") as f:
    content = f.read()
    
if "fun updateApiKey" not in content:
    content = content.replace('suspend fun insertEndpoint', '@Query("UPDATE endpoints SET apiKey = :apiKey WHERE id = :id")\n    suspend fun updateApiKey(id: Long, apiKey: String)\n\n    @Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insertEndpoint')

with open("app/src/main/java/com/example/data/EndpointDao.kt", "w") as f:
    f.write(content)

