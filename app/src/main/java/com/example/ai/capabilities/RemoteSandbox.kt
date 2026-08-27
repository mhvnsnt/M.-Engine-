package com.example.ai.capabilities

data class SandboxConfig(
    val limits: SandboxLimits,
    val networkPolicy: NetworkPolicy,
    val baseImage: String
)

data class SandboxLimits(
    val maxMemoryMb: Int,
    val maxCpuCores: Float,
    val maxExecutionMinutes: Int
)

enum class NetworkPolicy {
    ISOLATED,
    GITHUB_ONLY,
    OPEN_WITH_MONITORING
}

data class ExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timeoutTriggered: Boolean
)


interface RemoteSandboxManager {
    suspend fun provisionSandbox(jobId: String, config: SandboxConfig): String
    suspend fun destroySandbox(sandboxId: String): Boolean
    suspend fun cloneRepository(sandboxId: String, repo: RepositoryRef, secureToken: String): Boolean
    suspend fun executeCommand(sandboxId: String, command: String, timeoutMinutes: Int): ExecutionResult
}
