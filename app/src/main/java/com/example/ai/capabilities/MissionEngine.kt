package com.example.ai.capabilities

enum class MissionStatus {
    PLANNING,
    IN_PROGRESS,
    BLOCKED,
    BLOCKED_WAITING_PROVIDER,
    SUSPENDED_WAITING_RECOVERY,
    EVALUATING_EVIDENCE,
    ACHIEVED,
    ABORTED
}

data class MissionContext(
    val currentKnowledge: List<String>,
    val userPreferences: Map<String, String>,
    val knownConstraints: List<String>,
    val targetRepository: String? = null,
    val currentProvider: String? = null,
    val providerSwitchHistory: List<String> = emptyList()
)

data class MissionGoal(
    val description: String,
    val desiredOutcome: String,
    val evidenceRequirements: List<EvidenceType>
)

data class Subtask(
    val id: String,
    val description: String,
    var status: MissionStatus,
    var assignedJobId: String? = null,
    var assignedProvider: String? = null
)

data class Mission(
    val id: String,
    val name: String,
    val goal: MissionGoal,
    var context: MissionContext,
    val subtasks: List<Subtask>,
    val dependencies: List<String>,
    var currentState: MissionStatus = MissionStatus.PLANNING,
    val history: MutableList<String> = mutableListOf(),
    var lastExecutedStage: String? = null
)

interface MissionEngine {
    suspend fun createMission(prompt: String, contextEngine: PersonalContextEngine, targetRepo: String? = null): Mission
    suspend fun startMission(missionId: String): Boolean
    suspend fun updateMissionState(missionId: String, newState: MissionStatus)
    suspend fun updateMissionStage(missionId: String, stage: String)
    suspend fun recordProviderSwitch(missionId: String, fromProvider: String, toProvider: String, reason: String)
    suspend fun attachEvidence(missionId: String, evidence: EvidenceRecord)
    suspend fun evaluateMissionCompletion(missionId: String): Boolean
    suspend fun getMission(missionId: String): Mission?
}
