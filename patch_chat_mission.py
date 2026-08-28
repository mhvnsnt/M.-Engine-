import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    code = f.read()

# Add a fake active mission state
mission_state = """    private val _activeMission = MutableStateFlow<com.example.ai.capabilities.Mission?>(null)
    val activeMission: StateFlow<com.example.ai.capabilities.Mission?> = _activeMission
"""
code = code.replace("private val _isGenerating = MutableStateFlow(false)", mission_state + "\n    private val _isGenerating = MutableStateFlow(false)")

# Intercept message to create mission
mission_intercept = """
            if (text.lowercase().startsWith("fix ") || text.lowercase().startsWith("implement ") || text.lowercase().startsWith("research ")) {
                val mission = com.example.ai.capabilities.Mission(
                    id = "miss-${System.currentTimeMillis()}",
                    name = text,
                    goal = com.example.ai.capabilities.MissionGoal(
                        description = text,
                        desiredOutcome = "Verified working implementation",
                        evidenceRequirements = listOf(com.example.ai.capabilities.EvidenceType.RUNTIME_LOG, com.example.ai.capabilities.EvidenceType.BEHAVIORAL_EVIDENCE)
                    ),
                    context = com.example.ai.capabilities.MissionContext(emptyList(), emptyMap(), emptyList()),
                    subtasks = listOf(
                        com.example.ai.capabilities.Subtask("1", "Understand -> Retrieve -> Research", com.example.ai.capabilities.MissionStatus.PLANNING),
                        com.example.ai.capabilities.Subtask("2", "Plan -> Risk Assessment -> Implement", com.example.ai.capabilities.MissionStatus.PLANNING),
                        com.example.ai.capabilities.Subtask("3", "Build -> Run -> Observe", com.example.ai.capabilities.MissionStatus.PLANNING),
                        com.example.ai.capabilities.Subtask("4", "Diagnose -> Fix -> Retest", com.example.ai.capabilities.MissionStatus.PLANNING),
                        com.example.ai.capabilities.Subtask("5", "Compare -> Evidence -> Regression", com.example.ai.capabilities.MissionStatus.PLANNING)
                    ),
                    dependencies = emptyList()
                )
                _activeMission.value = mission
                
                val responseMsg = MessageEntity(
                    text = "Mission established: '${text}'. Outcome-oriented Universal Reality Loop engaged. Analyzing repository and planning durable job...",
                    isUser = false,
                    responderName = "M. Engine Mission Control",
                    groupId = groupId
                )
                repository.insertMessage(responseMsg)
                _isGenerating.value = false
                return@launch
            }
"""

code = code.replace("if (text.startsWith(\"/self-improve\")) {", mission_intercept + "\n                        if (text.startsWith(\"/self-improve\")) {")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(code)
