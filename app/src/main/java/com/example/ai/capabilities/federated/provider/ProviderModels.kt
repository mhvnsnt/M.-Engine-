package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState

enum class CapabilityType {
    CODING,
    DURABLE_WORKFLOW,
    BROWSER_AUTOMATION,
    SANDBOX_EXECUTION,
    MODEL_INFERENCE,
    RESEARCH_EXTRACTION,
    DEPLOYMENT,
    DATABASE,
    ARTIFACT_STORAGE
}

data class CapabilityProbeResult(
    val status: FabricNodeState,
    val details: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class CapabilityAuthorization(
    val governorSessionId: String,
    val maxBudgetTokens: Long? = null,
    val maxExecutionDurationMs: Long? = null,
    val allowedNetworkAccess: Boolean = false,
    val allowedPaths: List<String> = emptyList()
)

data class CapabilityTask(
    val taskId: String,
    val objective: String,
    val contextPayload: String, // JSON payload containing repository paths, issues, etc.
    val requiredEvidence: List<String> = emptyList() // E.g., ["git_diff", "test_results"]
)

data class CapabilityExecutionResult(
    val taskId: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val returnedEvidencePayload: String? = null, // JSON payload with diffs, URLs, etc.
    val costMetrics: CostMetrics? = null,
    val error: String? = null
)

data class CostMetrics(
    val tokensUsed: Long = 0,
    val executionTimeMs: Long = 0
)

