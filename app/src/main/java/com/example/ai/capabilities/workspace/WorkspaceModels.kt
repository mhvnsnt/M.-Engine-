package com.example.ai.capabilities.workspace

import java.util.UUID

data class Workspace(
    val workspaceId: String = UUID.randomUUID().toString(),
    val type: WorkspaceType,
    val title: String,
    val projectId: String?,
    val conversationIds: List<String> = emptyList(),
    val repositoryIds: List<String> = emptyList(),
    val artifactIds: List<String> = emptyList(),
    val activeAgentRuns: List<String> = emptyList(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)

enum class WorkspaceType {
    CONVERSATION,
    PROJECT,
    APP,
    GAME,
    REPOSITORY,
    RESEARCH,
    AGENT,
    OBSERVATORY,
    FABRIC,
    LIBRARY
}

data class Project(
    val projectId: String = UUID.randomUUID().toString(),
    val name: String,
    val repositoryIds: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val taskIds: List<String> = emptyList(),
    val workerIds: List<String> = emptyList(),
    val evidenceIds: List<String> = emptyList()
)

data class AgentSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val objective: String,
    val authorizedScope: String,
    val contextProvided: String,
    val tools: List<String>,
    val projectId: String? = null
)

data class AgentRun(
    val runId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val workerId: String,
    val status: String, // e.g. ACTIVE, PAUSED, COMPLETED, FAILED
    val currentAction: String?,
    val costTokens: Long = 0,
    val startTimeMs: Long = System.currentTimeMillis()
)

data class Artifact(
    val artifactId: String = UUID.randomUUID().toString(),
    val projectId: String?,
    val conversationId: String?,
    val type: ArtifactType,
    val version: Int = 1,
    val provenance: String, // Link to agent run or conversation that generated it
    val sourceWorker: String?,
    val sourceEvidence: String?,
    val previewUrl: String? = null,
    val repositoryLink: String? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ArtifactType {
    APP,
    GAME,
    WEBSITE,
    REPOSITORY,
    DOCUMENT,
    IMAGE,
    VIDEO,
    DATABASE,
    WORKFLOW,
    RESEARCH,
    CODE_PATCH
}

data class Preview(
    val previewId: String = UUID.randomUUID().toString(),
    val artifactId: String,
    val type: PreviewType,
    val url: String,
    val status: String
)

enum class PreviewType {
    WEB_PREVIEW,
    ANDROID_PREVIEW,
    GAME_PREVIEW,
    DOCUMENT_PREVIEW,
    IMAGE_PREVIEW,
    VIDEO_PREVIEW,
    DATA_PREVIEW,
    TERMINAL_PREVIEW,
    BROWSER_PREVIEW
}

