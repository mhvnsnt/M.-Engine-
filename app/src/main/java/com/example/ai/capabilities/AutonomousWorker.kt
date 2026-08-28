package com.example.ai.capabilities

import com.example.ai.PermissionLevel

enum class WorkerRole {
    CODER,
    REPO_ANALYSIS,
    RESEARCH,
    BROWSER,
    TERMINAL,
    DEVICE,
    VISUAL_VIDEO,
    TESTING,
    SECURITY,
    DOC_REVIEW
}

fun WorkerRole.toWorkloadType(): WorkloadType = when (this) {
    WorkerRole.CODER -> WorkloadType.CODING
    WorkerRole.REPO_ANALYSIS -> WorkloadType.REPOSITORY_COMPREHENSION
    WorkerRole.RESEARCH -> WorkloadType.RESEARCH
    WorkerRole.BROWSER -> WorkloadType.TOOL_USE
    WorkerRole.TERMINAL -> WorkloadType.DEBUGGING
    WorkerRole.DEVICE -> WorkloadType.UI_REASONING
    WorkerRole.VISUAL_VIDEO -> WorkloadType.VIDEO_MULTIMODAL
    WorkerRole.TESTING -> WorkloadType.SELF_CORRECTION
    WorkerRole.SECURITY -> WorkloadType.CODING
    WorkerRole.DOC_REVIEW -> WorkloadType.LONG_CONTEXT
}

data class WorkerCheckpoint(
    val stage: String,
    val partialOutput: String,
    val state: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkerDescriptor(
    val id: String,
    val name: String,
    val role: WorkerRole,
    val supportedWorkloads: List<WorkloadType>,
    val isLocal: Boolean,
    val permissionLevel: PermissionLevel,
    val capabilities: List<String>,
    val costProfile: CostProfile = CostProfile("LOW"),
    val reliabilityScore: Double = 1.0,
    val status: CapabilityStatus = CapabilityStatus.ONLINE
)

data class AutonomousWorkerTask(
    val taskId: String,
    val role: WorkerRole,
    val goal: String,
    val context: String,
    val parameters: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 60000L,
    val endpoints: List<com.example.data.EndpointEntity> = emptyList(),
    val checkpoint: WorkerCheckpoint? = null
)

data class AutonomousWorkerTaskResult(
    val taskId: String,
    val workerId: String,
    val workerRole: WorkerRole,
    val isSuccess: Boolean,
    val output: String,
    val latencyMs: Long,
    val evidenceRecordId: String? = null,
    val artifacts: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val providerUsed: String? = null,
    val failoverOccurred: Boolean = false,
    val checkpointSaved: WorkerCheckpoint? = null
)

interface AutonomousWorker {
    val descriptor: WorkerDescriptor
    suspend fun executeTask(task: AutonomousWorkerTask): AutonomousWorkerTaskResult
    suspend fun healthCheck(): Boolean = descriptor.status == CapabilityStatus.ONLINE
}
