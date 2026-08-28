package com.example.ai.capabilities

import com.example.data.MissionDao
import com.example.data.MissionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class MissionScheduleStatus {
    SCHEDULED,
    RUNNING,
    PAUSED,
    STOPPED_SUCCESS,
    STOPPED_BUDGET_EXHAUSTED,
    STOPPED_EVIDENCE_INSUFFICIENT,
    STOPPED_BLOCKED,
    STOPPED_RISK_EXCEEDED,
    STOPPED_CONVERGED,
    FAILED
}

data class ScheduledMissionRecord(
    val id: String,
    val name: String,
    val targetRepo: String,
    val goalDescription: String,
    val status: MissionScheduleStatus,
    val iterationCount: Int,
    val maxIterations: Int = 5,
    val budgetCostCents: Double = 0.0,
    val maxCostCents: Double = 50.0,
    val currentStageIndex: Int = 0,
    val currentStageName: String = "INITIALIZED",
    val checkpointState: Map<String, String> = emptyMap(),
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val terminationReason: String? = null
)

data class MissionExecutionResult(
    val missionId: String,
    val finalStatus: MissionScheduleStatus,
    val iterationsExecuted: Int,
    val totalCostCents: Double,
    val stageReached: String,
    val evidenceGathered: List<String>,
    val terminationReason: String,
    val deliveredArtifacts: Map<String, String> = emptyMap()
)

data class SchedulerStatusReport(
    val activeMissionsCount: Int,
    val completedMissionsCount: Int,
    val stoppedMissionsCount: Int,
    val scheduledRecords: List<ScheduledMissionRecord>,
    val totalBudgetConsumedCents: Double
)

interface DurableAutonomousScheduler {
    suspend fun scheduleMission(
        prompt: String,
        targetRepo: String = "mhvnsnt/M.-Engine-",
        maxIterations: Int = 5,
        maxCostCents: Double = 50.0
    ): ScheduledMissionRecord

    suspend fun updateCheckpoint(
        missionId: String,
        stageIndex: Int,
        stageName: String,
        checkpointData: Map<String, String>
    )

    suspend fun recordIteration(missionId: String, costCents: Double): Boolean

    suspend fun markTerminal(
        missionId: String,
        terminalStatus: MissionScheduleStatus,
        reason: String
    )

    suspend fun getMission(missionId: String): ScheduledMissionRecord?
    suspend fun getPendingMissions(): List<ScheduledMissionRecord>
    suspend fun resumePendingMissions(): List<ScheduledMissionRecord>
    suspend fun getSchedulerStatus(): SchedulerStatusReport
}

class DurableAutonomousSchedulerImpl(
    private val missionDao: MissionDao
) : DurableAutonomousScheduler {

    private val localMemoryCache = ConcurrentHashMap<String, ScheduledMissionRecord>()

    override suspend fun scheduleMission(
        prompt: String,
        targetRepo: String,
        maxIterations: Int,
        maxCostCents: Double
    ): ScheduledMissionRecord = withContext(Dispatchers.IO) {
        val missionId = "miss-sched-${System.currentTimeMillis()}"
        val record = ScheduledMissionRecord(
            id = missionId,
            name = prompt,
            targetRepo = targetRepo,
            goalDescription = "Autonomous Mission: $prompt",
            status = MissionScheduleStatus.SCHEDULED,
            iterationCount = 0,
            maxIterations = maxIterations.coerceIn(1, 20),
            budgetCostCents = 0.0,
            maxCostCents = maxCostCents,
            currentStageIndex = 0,
            currentStageName = "SCHEDULED",
            checkpointState = mapOf("prompt" to prompt, "targetRepo" to targetRepo),
            createdTimestamp = System.currentTimeMillis(),
            lastHeartbeat = System.currentTimeMillis()
        )

        localMemoryCache[missionId] = record

        // Persist into Room database
        val entity = recordToEntity(record)
        missionDao.insertMission(entity)

        record
    }

    override suspend fun updateCheckpoint(
        missionId: String,
        stageIndex: Int,
        stageName: String,
        checkpointData: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val current = getMission(missionId) ?: return@withContext
        val updated = current.copy(
            currentStageIndex = stageIndex,
            currentStageName = stageName,
            status = MissionScheduleStatus.RUNNING,
            checkpointState = current.checkpointState + checkpointData,
            lastHeartbeat = System.currentTimeMillis()
        )
        localMemoryCache[missionId] = updated
        missionDao.updateMission(recordToEntity(updated))
    }

    override suspend fun recordIteration(missionId: String, costCents: Double): Boolean = withContext(Dispatchers.IO) {
        val current = getMission(missionId) ?: return@withContext false
        val newIterationCount = current.iterationCount + 1
        val newCost = current.budgetCostCents + costCents

        // Check if budget is exhausted
        val budgetExhausted = newIterationCount >= current.maxIterations || newCost >= current.maxCostCents

        val updated = current.copy(
            iterationCount = newIterationCount,
            budgetCostCents = newCost,
            status = if (budgetExhausted) MissionScheduleStatus.STOPPED_BUDGET_EXHAUSTED else current.status,
            terminationReason = if (budgetExhausted) "Iteration budget or compute cost limit reached" else current.terminationReason,
            lastHeartbeat = System.currentTimeMillis()
        )

        localMemoryCache[missionId] = updated
        missionDao.updateMission(recordToEntity(updated))
        !budgetExhausted
    }

    override suspend fun markTerminal(
        missionId: String,
        terminalStatus: MissionScheduleStatus,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val current = getMission(missionId) ?: return@withContext
        val updated = current.copy(
            status = terminalStatus,
            terminationReason = reason,
            lastHeartbeat = System.currentTimeMillis()
        )
        localMemoryCache[missionId] = updated
        missionDao.updateMission(recordToEntity(updated))
    }

    override suspend fun getMission(missionId: String): ScheduledMissionRecord? = withContext(Dispatchers.IO) {
        localMemoryCache[missionId]?.let { return@withContext it }
        val entity = missionDao.getMission(missionId) ?: return@withContext null
        val record = entityToRecord(entity)
        localMemoryCache[missionId] = record
        record
    }

    override suspend fun getPendingMissions(): List<ScheduledMissionRecord> = withContext(Dispatchers.IO) {
        val all = missionDao.getAllMissions().map { entityToRecord(it) }
        all.filter { it.status == MissionScheduleStatus.SCHEDULED || it.status == MissionScheduleStatus.RUNNING }
    }

    override suspend fun resumePendingMissions(): List<ScheduledMissionRecord> = withContext(Dispatchers.IO) {
        val pending = getPendingMissions()
        pending.forEach { record ->
            localMemoryCache[record.id] = record
        }
        pending
    }

    override suspend fun getSchedulerStatus(): SchedulerStatusReport = withContext(Dispatchers.IO) {
        val all = missionDao.getAllMissions().map { entityToRecord(it) }
        val active = all.count { it.status == MissionScheduleStatus.SCHEDULED || it.status == MissionScheduleStatus.RUNNING }
        val completed = all.count { it.status == MissionScheduleStatus.STOPPED_SUCCESS }
        val stopped = all.count {
            it.status in listOf(
                MissionScheduleStatus.STOPPED_BUDGET_EXHAUSTED,
                MissionScheduleStatus.STOPPED_EVIDENCE_INSUFFICIENT,
                MissionScheduleStatus.STOPPED_BLOCKED,
                MissionScheduleStatus.STOPPED_RISK_EXCEEDED,
                MissionScheduleStatus.STOPPED_CONVERGED
            )
        }
        val totalCost = all.sumOf { it.budgetCostCents }

        SchedulerStatusReport(
            activeMissionsCount = active,
            completedMissionsCount = completed,
            stoppedMissionsCount = stopped,
            scheduledRecords = all,
            totalBudgetConsumedCents = totalCost
        )
    }

    private fun escapeStr(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    private fun unescapeStr(s: String): String = s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")

    private fun recordToEntity(r: ScheduledMissionRecord): MissionEntity {
        val cpPairs = r.checkpointState.entries.joinToString(",") { (k, v) ->
            "\"${escapeStr(k)}\":\"${escapeStr(v)}\""
        }
        val historyJson = """{"targetRepo":"${escapeStr(r.targetRepo)}","iterationCount":${r.iterationCount},"maxIterations":${r.maxIterations},"budgetCostCents":${r.budgetCostCents},"maxCostCents":${r.maxCostCents},"currentStageIndex":${r.currentStageIndex},"currentStageName":"${escapeStr(r.currentStageName)}","terminationReason":"${escapeStr(r.terminationReason ?: "")}","status":"${r.status.name}","checkpointState":{$cpPairs}}"""

        val missionStatus = when (r.status) {
            MissionScheduleStatus.SCHEDULED, MissionScheduleStatus.RUNNING -> MissionStatus.IN_PROGRESS
            MissionScheduleStatus.STOPPED_SUCCESS -> MissionStatus.ACHIEVED
            MissionScheduleStatus.STOPPED_BLOCKED -> MissionStatus.BLOCKED
            MissionScheduleStatus.PAUSED -> MissionStatus.SUSPENDED_WAITING_RECOVERY
            else -> MissionStatus.ABORTED
        }

        return MissionEntity(
            id = r.id,
            name = r.name,
            goalDescription = r.goalDescription,
            desiredOutcome = "Verified autonomous persistent execution with scoped evidence",
            currentState = missionStatus,
            historyJson = historyJson,
            subtasksJson = "[]"
        )
    }

    private fun entityToRecord(e: MissionEntity): ScheduledMissionRecord {
        var targetRepo = "mhvnsnt/M.-Engine-"
        var iterationCount = 0
        var maxIterations = 5
        var budgetCostCents = 0.0
        var maxCostCents = 50.0
        var currentStageIndex = 0
        var currentStageName = "INITIALIZED"
        var terminationReason: String? = null
        var scheduleStatus = when (e.currentState) {
            MissionStatus.ACHIEVED -> MissionScheduleStatus.STOPPED_SUCCESS
            MissionStatus.BLOCKED -> MissionScheduleStatus.STOPPED_BLOCKED
            MissionStatus.SUSPENDED_WAITING_RECOVERY -> MissionScheduleStatus.PAUSED
            MissionStatus.IN_PROGRESS -> MissionScheduleStatus.RUNNING
            else -> MissionScheduleStatus.SCHEDULED
        }
        val checkpointMap = mutableMapOf<String, String>()

        try {
            val json = e.historyJson
            if (json.isNotBlank()) {
                val strPattern = Regex(""""([a-zA-Z0-9_]+)"\s*:\s*"([^"]*)"""")
                val numPattern = Regex(""""([a-zA-Z0-9_]+)"\s*:\s*([0-9.]+)""")

                strPattern.findAll(json).forEach { m ->
                    val key = m.groupValues[1]
                    val value = unescapeStr(m.groupValues[2])
                    when (key) {
                        "targetRepo" -> targetRepo = value
                        "currentStageName" -> currentStageName = value
                        "terminationReason" -> if (value.isNotBlank()) terminationReason = value
                        "status" -> scheduleStatus = try { MissionScheduleStatus.valueOf(value) } catch (_: Exception) { scheduleStatus }
                    }
                }

                numPattern.findAll(json).forEach { m ->
                    val key = m.groupValues[1]
                    val value = m.groupValues[2]
                    when (key) {
                        "iterationCount" -> iterationCount = value.toIntOrNull() ?: iterationCount
                        "maxIterations" -> maxIterations = value.toIntOrNull() ?: maxIterations
                        "budgetCostCents" -> budgetCostCents = value.toDoubleOrNull() ?: budgetCostCents
                        "maxCostCents" -> maxCostCents = value.toDoubleOrNull() ?: maxCostCents
                        "currentStageIndex" -> currentStageIndex = value.toIntOrNull() ?: currentStageIndex
                    }
                }

                if (json.contains("\"checkpointState\":{")) {
                    val cpPart = json.substringAfter("\"checkpointState\":{").substringBefore("}")
                    strPattern.findAll(cpPart).forEach { m ->
                        checkpointMap[m.groupValues[1]] = unescapeStr(m.groupValues[2])
                    }
                }
            }
        } catch (_: Throwable) {}

        return ScheduledMissionRecord(
            id = e.id,
            name = e.name,
            targetRepo = targetRepo,
            goalDescription = e.goalDescription,
            status = scheduleStatus,
            iterationCount = iterationCount,
            maxIterations = maxIterations,
            budgetCostCents = budgetCostCents,
            maxCostCents = maxCostCents,
            currentStageIndex = currentStageIndex,
            currentStageName = currentStageName,
            checkpointState = checkpointMap,
            terminationReason = terminationReason
        )
    }
}
