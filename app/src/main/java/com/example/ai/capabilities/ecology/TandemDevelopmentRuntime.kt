package com.example.ai.capabilities.ecology

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class DevelopmentSignalType {
    NEW_REQUIREMENT,
    ARCHITECTURAL_DECISION,
    CORRECTION,
    PRIORITY_CHANGE,
    PROJECT_GOAL,
    IDEA,
    QUESTION,
    CONSTRAINT,
    EXTERNAL_DISCOVERY
}

enum class SignalStatus {
    RECEIVED,
    REQUIRES_RESEARCH,
    HYPOTHESIZING,
    EXPERIMENTING,
    COMPLETED,
    REJECTED
}

data class DevelopmentSignal(
    val id: String = UUID.randomUUID().toString().take(8),
    val type: DevelopmentSignalType,
    val project: String,
    val intent: String,
    val confidence: Double = 0.9,
    var status: SignalStatus = SignalStatus.RECEIVED,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveWorkerRecord(
    val workerId: String,
    val workerType: String,
    val currentTask: String,
    val status: String,
    val artifactsCount: Int = 0,
    val blockedBy: String? = null
)

data class TandemRuntimeState(
    val isRunning: Boolean = true,
    val currentObjective: String = "Idle - Listening for Development Signals & Ecology Changes",
    val currentPhase: String = "OBSERVING",
    val activeWorkers: List<ActiveWorkerRecord> = emptyList(),
    val totalEvidenceArtifacts: Int = 0,
    val nextBestAction: String = "Await next scheduled wake or owner development signal",
    val requiresOwnerInterruption: Boolean = false,
    val pendingApprovalDescription: String? = null
)

object SharedDevelopmentMemory {
    private val _signals = MutableStateFlow<List<DevelopmentSignal>>(emptyList())
    val signals: StateFlow<List<DevelopmentSignal>> = _signals.asStateFlow()

    private val _runtimeState = MutableStateFlow(TandemRuntimeState())
    val runtimeState: StateFlow<TandemRuntimeState> = _runtimeState.asStateFlow()

    private val _evidenceHistory = MutableStateFlow<List<CapabilityResult>>(emptyList())
    val evidenceHistory: StateFlow<List<CapabilityResult>> = _evidenceHistory.asStateFlow()

    private val _causalRecords = MutableStateFlow<List<CausalDevelopmentRecord>>(emptyList())
    val causalRecords: StateFlow<List<CausalDevelopmentRecord>> = _causalRecords.asStateFlow()

    private val _mindstream = MutableStateFlow<List<String>>(emptyList())
    val mindstream: StateFlow<List<String>> = _mindstream.asStateFlow()

    fun ingestMindstream(entry: String) {
        val current = _mindstream.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $entry")
        _mindstream.value = current.take(200)
    }

    fun ingestSignal(signal: DevelopmentSignal) {
        val updated = _signals.value.toMutableList()
        updated.add(0, signal)
        _signals.value = updated
        println("SHARED MEMORY: Ingested human signal [${signal.type}] for project '${signal.project}': ${signal.intent}")
    }

    fun updateSignalStatus(signalId: String, status: SignalStatus) {
        _signals.value = _signals.value.map {
            if (it.id == signalId) it.copy(status = status) else it
        }
    }

    fun updateRuntimeState(state: TandemRuntimeState) {
        _runtimeState.value = state
    }

    fun recordEvidence(result: CapabilityResult) {
        val current = _evidenceHistory.value.toMutableList()
        current.add(0, result)
        _evidenceHistory.value = current
    }

    fun recordCausalLink(record: CausalDevelopmentRecord) {
        val current = _causalRecords.value.toMutableList()
        current.add(0, record)
        _causalRecords.value = current
    }

    fun clear() {
        _signals.value = emptyList()
        _evidenceHistory.value = emptyList()
        _causalRecords.value = emptyList()
        _runtimeState.value = TandemRuntimeState()
    }
}

class TandemDevelopmentRuntime(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val memory: SharedDevelopmentMemory = SharedDevelopmentMemory
) {
    suspend fun processPendingSignalsAndEcology(
        budget: ExecutionBudget = ExecutionBudget(maxIterations = 5, maxParallelWorkers = 3)
    ): LoopResult {
        val pendingSignals = memory.signals.value.filter {
            it.status == SignalStatus.RECEIVED || it.status == SignalStatus.REQUIRES_RESEARCH
        }

        if (pendingSignals.isNotEmpty()) {
            val signal = pendingSignals.first()
            println("TANDEM RUNTIME: Processing Signal [${signal.type}] '${signal.intent}' for project '${signal.project}'")
            
            memory.updateSignalStatus(signal.id, SignalStatus.REQUIRES_RESEARCH)
            memory.updateRuntimeState(
                TandemRuntimeState(
                    isRunning = true,
                    currentObjective = "${signal.project}: ${signal.intent}",
                    currentPhase = "RESEARCHING",
                    activeWorkers = listOf(
                        ActiveWorkerRecord("worker-1", "RepositoryInspector", "Inspect ${signal.project} architecture", "ACTIVE"),
                        ActiveWorkerRecord("worker-2", "ResearchWorker", "Research reference mechanics for ${signal.intent}", "ACTIVE"),
                        ActiveWorkerRecord("worker-3", "SandboxWorker", "Identify test harness", "ACTIVE")
                    ),
                    totalEvidenceArtifacts = memory.evidenceHistory.value.size,
                    nextBestAction = "Synthesize evidence and formulate candidate experiment patch",
                    requiresOwnerInterruption = false
                )
            )

            // Select relevant capabilities to spawn
            val capabilities = listOf(
                GitHubWorkerCapability(),
                WebResearchCapability(),
                DocumentationCapability(),
                CodingWorkerCapability(),
                SandboxExecutionCapability()
            )

            // Perform initial verification check so capabilities become available if unverified
            for (cap in capabilities) {
                if (!cap.isAvailable()) {
                    cap.verifyHealth()
                }
            }

            val loop = AutonomousExecutionLoop(
                budget = budget,
                capabilities = capabilities,
                objective = "${signal.project}: ${signal.intent}"
            )
            val wakeRecord = MetabolismWakeRecord(
                scheduledTimestamp = System.currentTimeMillis(),
                actualStartTimestamp = System.currentTimeMillis(),
                networkAvailable = true,
                schedulingJitterMs = 0L,
                scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
            )

            val loopResult = loop.run(wakeRecord)
            
            // Persist gathered evidence and causal linkages into shared memory
            for (res in loopResult.capabilityResults) {
                memory.recordEvidence(res)
                memory.recordCausalLink(
                    CausalDevelopmentRecord(
                        humanSignalId = signal.id,
                        humanSignalType = signal.type.name,
                        humanIntent = signal.intent,
                        opportunityId = "opp-${signal.id}",
                        opportunityDescription = "Research and implement candidate solution for ${signal.intent}",
                        dispatchedWorkerId = "worker-${res.authorizationUsed ?: signal.id}",
                        capabilityId = res.authorizationUsed,
                        experimentDescription = res.observations.firstOrNull() ?: "Executed worker capability",
                        evidenceArtifact = res.artifactsGenerated.firstOrNull(),
                        proposedPatch = if (res.artifactsGenerated.any { it.endsWith(".diff") || it.endsWith(".patch") }) res.artifactsGenerated.first { it.endsWith(".diff") || it.endsWith(".patch") } else null,
                        verificationOutcome = if (res.success) "VERIFIED" else "FAILED"
                    )
                )
            }

            memory.updateSignalStatus(signal.id, SignalStatus.HYPOTHESIZING)
            memory.updateRuntimeState(
                TandemRuntimeState(
                    isRunning = false,
                    currentObjective = "${signal.project}: ${signal.intent}",
                    currentPhase = "EXPERIMENTING",
                    activeWorkers = emptyList(),
                    totalEvidenceArtifacts = memory.evidenceHistory.value.size,
                    nextBestAction = "Review candidate patch and await experiment execution authorization",
                    requiresOwnerInterruption = false
                )
            )

            return loopResult
        } else {
            // General ecology maintenance wake
            val capabilities = FederatedCapabilityRegistry.getAllCapabilities()
            for (cap in capabilities) {
                if (!cap.isAvailable()) {
                    cap.verifyHealth()
                }
            }
            val loop = AutonomousExecutionLoop(budget, capabilities)
            val wakeRecord = MetabolismWakeRecord(
                scheduledTimestamp = System.currentTimeMillis(),
                actualStartTimestamp = System.currentTimeMillis(),
                networkAvailable = true,
                schedulingJitterMs = 0L,
                scheduleStatus = WakeScheduleStatus.ON_SCHEDULE
            )
            return loop.run(wakeRecord)
        }
    }
}
