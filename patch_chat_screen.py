import re

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "r") as f:
    code = f.read()

mission_ui = """    val activeMission by viewModel.activeMission.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        activeMission?.let { mission ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = "Mission")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Mission: ${mission.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${mission.currentState.name}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { 0.2f })
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Universal Reality Loop Phases:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    mission.subtasks.forEach { task ->
                        Text("- ${task.description} [${task.status.name}]", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
"""

if "activeMission" not in code:
    code = code.replace("Column(modifier = Modifier.fillMaxSize()) {", mission_ui)

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "w") as f:
    f.write(code)
