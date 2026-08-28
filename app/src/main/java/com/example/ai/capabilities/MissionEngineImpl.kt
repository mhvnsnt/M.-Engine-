package com.example.ai.capabilities

import com.example.data.MissionDao
import com.example.data.MissionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MissionEngineImpl(private val missionDao: MissionDao) : MissionEngine {

    override suspend fun createMission(
        prompt: String,
        contextEngine: PersonalContextEngine,
        targetRepo: String?
    ): Mission {
        val missionId = "miss-${System.currentTimeMillis()}"
        val mission = Mission(
            id = missionId,
            name = prompt,
            goal = MissionGoal(
                description = prompt,
                desiredOutcome = "Verified working implementation with runtime & regression evidence",
                evidenceRequirements = listOf(EvidenceType.RUNTIME_LOG, EvidenceType.UNIT_TEST, EvidenceType.BENCHMARK)
            ),
            context = MissionContext(
                currentKnowledge = emptyList(),
                userPreferences = emptyMap(),
                knownConstraints = listOf("Reality Contract strictly active", "No synthetic mocks in production verification"),
                targetRepository = targetRepo
            ),
            subtasks = listOf(
                Subtask("1", "Understand -> Retrieve -> Research", MissionStatus.PLANNING),
                Subtask("2", "Plan -> Risk Assessment -> Implement", MissionStatus.PLANNING),
                Subtask("3", "Build -> Run -> Observe", MissionStatus.PLANNING),
                Subtask("4", "Reproduce -> Diagnose -> Fix", MissionStatus.PLANNING),
                Subtask("5", "Retest -> Compare -> Gather Evidence -> Regression", MissionStatus.PLANNING),
                Subtask("6", "Review -> Deliver", MissionStatus.PLANNING)
            ),
            dependencies = emptyList(),
            currentState = MissionStatus.PLANNING
        )

        val entity = MissionEntity(
            id = missionId,
            name = mission.name,
            goalDescription = mission.goal.description,
            desiredOutcome = mission.goal.desiredOutcome,
            currentState = mission.currentState,
            historyJson = "[]",
            subtasksJson = "[]"
        )
        withContext(Dispatchers.IO) {
            missionDao.insertMission(entity)
        }
        return mission
    }

    override suspend fun startMission(missionId: String): Boolean {
        updateMissionState(missionId, MissionStatus.IN_PROGRESS)
        return true
    }

    override suspend fun updateMissionState(missionId: String, newState: MissionStatus) {
        withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId)
            if (entity != null) {
                missionDao.updateMission(entity.copy(currentState = newState))
            }
        }
    }

    override suspend fun updateMissionStage(missionId: String, stage: String) {
        withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId)
            if (entity != null) {
                val currentHistory = parseHistory(entity.historyJson)
                currentHistory.add("STAGE_TRANSITION: $stage at ${System.currentTimeMillis()}")
                missionDao.updateMission(entity.copy(historyJson = serializeHistory(currentHistory)))
            }
        }
    }

    override suspend fun recordProviderSwitch(
        missionId: String,
        fromProvider: String,
        toProvider: String,
        reason: String
    ) {
        withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId)
            if (entity != null) {
                val currentHistory = parseHistory(entity.historyJson)
                val logEntry = "PROVIDER_FAILOVER: from=$fromProvider to=$toProvider reason=$reason time=${System.currentTimeMillis()}"
                currentHistory.add(logEntry)
                missionDao.updateMission(entity.copy(historyJson = serializeHistory(currentHistory)))
            }
        }
    }

    override suspend fun attachEvidence(missionId: String, evidence: EvidenceRecord) {
        withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId)
            if (entity != null) {
                val currentHistory = parseHistory(entity.historyJson)
                currentHistory.add("EVIDENCE:${evidence.id}:${evidence.evidenceType}:${evidence.level}")
                missionDao.updateMission(entity.copy(historyJson = serializeHistory(currentHistory)))
            }
        }
    }

    override suspend fun evaluateMissionCompletion(missionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId)
            entity?.currentState == MissionStatus.ACHIEVED
        }
    }

    override suspend fun getMission(missionId: String): Mission? {
        return withContext(Dispatchers.IO) {
            val entity = missionDao.getMission(missionId) ?: return@withContext null
            val historyList = parseHistory(entity.historyJson)

            Mission(
                id = entity.id,
                name = entity.name,
                goal = MissionGoal(entity.goalDescription, entity.desiredOutcome, listOf(EvidenceType.RUNTIME_LOG)),
                context = MissionContext(emptyList(), emptyMap(), emptyList()),
                subtasks = emptyList(),
                dependencies = emptyList(),
                currentState = entity.currentState,
                history = historyList
            )
        }
    }

    private fun parseHistory(json: String): MutableList<String> {
        val list = mutableListOf<String>()
        if (json.isBlank() || json == "[]") return list
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            return list
        } catch (_: Throwable) {
            // Fallback for JVM unit tests without android.os mocks
            val cleaned = json.removePrefix("[").removeSuffix("]").trim()
            if (cleaned.isNotEmpty()) {
                cleaned.split(";;;").forEach { item ->
                    val trimmed = item.trim().removeSurrounding("\"")
                    if (trimmed.isNotEmpty()) list.add(trimmed)
                }
            }
            return list
        }
    }

    private fun serializeHistory(list: List<String>): String {
        return try {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            arr.toString()
        } catch (_: Throwable) {
            // Fallback serialization
            "[" + list.joinToString(";;;") { "\"$it\"" } + "]"
        }
    }
}
