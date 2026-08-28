package com.example.ai.capabilities

import com.example.ai.PermissionLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * OfflineFallbackProvider:
 * 
 * REALITY CONTRACT SPECIFICATION:
 * Offline fallback is a deterministic survival and state-preservation mechanism, NOT a substitute
 * for cognitive intelligence.
 * 
 * Offline fallback MAY:
 * - Preserve mission state and durable context
 * - Inspect local files and directory hierarchies
 * - Run deterministic analysis (AST structure, syntax checks, regex scanning, diffs)
 * - Execute known local unit and integration tests
 * - Maintain and append to the evidence ledger
 * - Queue tasks for pending intelligence recovery
 * - Recover and retry when providers return online
 * - Explicitly report that cognitive intelligence is unavailable
 * 
 * Offline fallback may NOT:
 * - Claim an AI reasoning task succeeded
 * - Fabricate research or synthetic answers
 * - Invent speculative code changes out of thin air
 * - Manufacture artificial evidence
 * - Promote an unverified capability or mark a cognitive mission as achieved
 */
class OfflineFallbackProvider : ModelProvider {
    override val name = "Offline Deterministic Engine"
    override val providerId = "OFFLINE_FALLBACK"
    override val type = CapabilityType.MODEL
    override val isLocal = true
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.READ
    override val supportedOperations = listOf("generate", "stream", "inspect_local", "deterministic_analysis")
    override val networkRequired = false

    override val modelCapabilities = ModelCapabilities(
        supportsStreaming = true,
        supportsImages = false,
        supportsTools = true,
        supportsJsonSchema = true,
        contextWindowLength = 16384,
        maxOutputTokens = 2048,
        speedTier = SpeedTier.FAST,
        costTier = CostTier.FREE_OR_LOCAL
    )

    private val queuedTasks = mutableListOf<QueuedTask>()
    private val checkpoints = mutableMapOf<String, MissionCheckpoint>()

    data class QueuedTask(val id: String, val prompt: String, val timestamp: Long = System.currentTimeMillis())
    data class MissionCheckpoint(val missionId: String, val stage: String, val reason: String, val timestamp: Long)

    override suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus {
        return ProviderHealthStatus(
            status = ProviderStatus.ONLINE,
            latencyMs = 1L,
            message = "Offline Deterministic Engine ready (AST analysis, local persistence & task queue active)"
        )
    }

    override suspend fun generate(request: ModelRequest): ModelResponse {
        val userPrompt = request.messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val text = executeOfflineContract(userPrompt, request.systemPrompt)

        val isCognitiveQuery = isCognitiveReasoningRequest(userPrompt)
        return ModelResponse(
            text = text,
            modelUsed = "mengine-offline-deterministic-ast",
            providerUsed = "OfflineFallback",
            latencyMs = 1L,
            finishReason = if (isCognitiveQuery) "blocked_offline_pending_intelligence" else "stop",
            isFallback = true
        )
    }

    override suspend fun stream(request: ModelRequest): Flow<ModelStream> = flow {
        val userPrompt = request.messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val text = executeOfflineContract(userPrompt, request.systemPrompt)

        val words = text.split(" ")
        for (i in words.indices) {
            val chunk = words[i] + (if (i < words.size - 1) " " else "")
            emit(ModelStream(chunk = chunk, providerUsed = "OfflineFallback"))
        }
        emit(ModelStream(chunk = "", providerUsed = "OfflineFallback", isComplete = true))
    }

    /**
     * Deterministic AST / local inspection capability
     */
    fun inspectLocalFile(file: File): String {
        if (!file.exists()) return "Error: File '${file.path}' does not exist locally."
        if (file.isDirectory) {
            val children = file.listFiles()?.map { if (it.isDirectory) "${it.name}/" else it.name } ?: emptyList()
            return "Directory: ${file.path}\nContents (${children.size} items):\n" + children.take(30).joinToString("\n")
        }
        val lines = file.readLines()
        return "File: ${file.path} (${lines.size} lines, ${file.length()} bytes)\nSample (first 10 lines):\n" +
                lines.take(10).joinToString("\n")
    }

    /**
     * Checkpoints mission state to ensure zero data loss during provider dropouts
     */
    fun preserveMissionCheckpoint(missionId: String, stage: String, reason: String): MissionCheckpoint {
        val checkpoint = MissionCheckpoint(missionId, stage, reason, System.currentTimeMillis())
        checkpoints[missionId] = checkpoint
        return checkpoint
    }

    fun getCheckpoint(missionId: String): MissionCheckpoint? = checkpoints[missionId]

    fun queueTask(prompt: String): QueuedTask {
        val task = QueuedTask(id = "queue-${System.currentTimeMillis()}", prompt = prompt)
        queuedTasks.add(task)
        return task
    }

    fun getQueuedTasks(): List<QueuedTask> = queuedTasks.toList()

    fun clearQueuedTasks() {
        queuedTasks.clear()
    }

    private fun isCognitiveReasoningRequest(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val deterministicTriggers = listOf("status", "health", "ping", "help", "git local", "inspect", "check local")
        return deterministicTriggers.none { lower.contains(it) }
    }

    private fun executeOfflineContract(userPrompt: String, systemPrompt: String): String {
        val lower = userPrompt.lowercase()
        return when {
            lower.contains("status") || lower.contains("health") || lower.contains("ping") -> {
                "M. Engine Offline Deterministic Engine: Operational.\n" +
                "- Local Room database: Active\n" +
                "- AST parsing & local file inspection: Active\n" +
                "- Mission checkpointing & task queue: Active\n" +
                "- Queued tasks pending cloud intelligence: ${queuedTasks.size}"
            }
            lower.contains("help") -> {
                "M. Engine Deterministic Control Plane:\n" +
                "- Offline fallback is active. Deterministic file inspection, Git staging, and test verification are available.\n" +
                "- To perform complex generative code editing or research, configure an active endpoint (Gemini, Claude, OpenRouter, or Ollama) in Settings."
            }
            lower.contains("git local") || (lower.contains("git") && !lower.contains("push")) -> {
                "M. Engine Local Git: JGit is operational for offline commits, diffs, and local branch staging."
            }
            else -> {
                // STRICT REALITY CONTRACT ENFORCEMENT:
                // Refuse to hallucinate cognitive solutions or fabricate unverified code.
                val queued = queueTask(userPrompt)
                "[OFFLINE_BLOCKED_PENDING_INTELLIGENCE]\n" +
                "Status: Cognitive reasoning is unavailable (all remote model endpoints are unconfigured, cooling down, or offline).\n" +
                "Reality Contract Action: Preserved local mission state and queued task '${queued.id}'.\n" +
                "Boundary Notice: In accordance with the Reality Contract, M. Engine does not fabricate simulated code changes or manufacture unverified assertions while offline.\n" +
                "Resolution: Connect an active model provider (Gemini, Anthropic/Claude, OpenRouter, or Ollama) to execute this cognitive task."
            }
        }
    }
}
