package com.example.ai

enum class PermissionLevel {
    READ,
    LOW_RISK_WRITE,
    HIGH_RISK_WRITE,
    SYSTEM,
    DESTRUCTIVE
}

data class ToolRequest(
    val toolName: String,
    val parameters: Map<String, String>,
    val permissionLevel: PermissionLevel
)

data class AgentStep(
    val description: String,
    val toolRequest: ToolRequest? = null
)

data class AgentPlan(
    val goal: String,
    val steps: List<AgentStep>,
    val requiresApproval: Boolean,
    val rawResponse: String = ""
)

data class ToolResult(
    val request: ToolRequest,
    val success: Boolean,
    val output: String
)

data class AgentResult(
    val plan: AgentPlan,
    val executionResults: List<ToolResult>,
    val finalSummary: String
)
