package com.example.ai.capabilities

import com.example.ai.PermissionLevel


data class TaskRequirements(
    val languages: List<String>,
    val requiresBrowser: Boolean,
    val requiresDeepReasoning: Boolean,
    val costConstraint: CostProfile
)


interface WorkerSelector {
    suspend fun selectBestWorker(
        requirements: TaskRequirements, 
        capabilityKnowledge: Map<String, CandidateEvaluation>
    ): CodingWorkerRuntime
}

abstract class CodingWorkerRuntime(
    val sandboxManager: RemoteSandboxManager,
    val sandboxId: String
) : CodingAgentRuntime {
    protected suspend fun runInSandbox(command: String): ExecutionResult {
        return sandboxManager.executeCommand(sandboxId, command, timeoutMinutes = 15)
    }
}

class AiderRuntime(sandboxManager: RemoteSandboxManager, sandboxId: String) : CodingWorkerRuntime(sandboxManager, sandboxId) {
    override val name = "Aider"
    override val type = CapabilityType.REMOTE_AGENT
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.LOW_RISK_WRITE
    override val supportedOperations = listOf("inspect", "plan", "modify", "build", "test", "review")
    override val networkRequired = true
    override val costProfile = CostProfile("MEDIUM")
    override val supportedLanguages = listOf("python", "javascript", "typescript", "java", "kotlin", "cpp")
    
    override suspend fun inspect(repository: String): String = runInSandbox("aider --message 'Analyze repo structure' --no-commit").stdout
    override suspend fun plan(task: String, context: String): String = runInSandbox("aider --message 'Plan: $task' --no-commit").stdout
    override suspend fun modify(plan: String): Boolean = runInSandbox("aider --message 'Implement: $plan' --yes").exitCode == 0
    override suspend fun build(): String = runInSandbox("./gradlew assembleDebug").stdout
    override suspend fun test(): String = runInSandbox("./gradlew testDebugUnitTest").stdout
    override suspend fun review(diff: String): Boolean = runInSandbox("aider --message 'Review diff' --no-commit").exitCode == 0
    override suspend fun cancel(): Boolean = sandboxManager.destroySandbox(sandboxId)
}

class MiniSWEAgentRuntime(sandboxManager: RemoteSandboxManager, sandboxId: String) : CodingWorkerRuntime(sandboxManager, sandboxId) {
    override val name = "MiniSWEAgent"
    override val type = CapabilityType.REMOTE_AGENT
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.LOW_RISK_WRITE
    override val supportedOperations = listOf("inspect", "plan", "modify", "build", "test", "review")
    override val networkRequired = true
    override val costProfile = CostProfile("MEDIUM")
    override val supportedLanguages = listOf("python", "java", "kotlin", "go", "rust")
    
    override suspend fun inspect(repository: String): String = runInSandbox("python -m miniswe.inspect").stdout
    override suspend fun plan(task: String, context: String): String = runInSandbox("python -m miniswe.plan '$task'").stdout
    override suspend fun modify(plan: String): Boolean = runInSandbox("python -m miniswe.modify").exitCode == 0
    override suspend fun build(): String = runInSandbox("make build").stdout
    override suspend fun test(): String = runInSandbox("make test").stdout
    override suspend fun review(diff: String): Boolean = runInSandbox("python -m miniswe.review").exitCode == 0
    override suspend fun cancel(): Boolean = sandboxManager.destroySandbox(sandboxId)
}

class OpenHandsRuntime(sandboxManager: RemoteSandboxManager, sandboxId: String) : CodingWorkerRuntime(sandboxManager, sandboxId) {
    override val name = "OpenHands"
    override val type = CapabilityType.REMOTE_AGENT
    override val isLocal = false
    override val status = CapabilityStatus.ONLINE
    override val permissionLevel = PermissionLevel.LOW_RISK_WRITE
    override val supportedOperations = listOf("inspect", "plan", "modify", "build", "test", "review")
    override val networkRequired = true
    override val costProfile = CostProfile("MEDIUM")
    override val supportedLanguages = listOf("all")
    
    override suspend fun inspect(repository: String): String = runInSandbox("openhands inspect").stdout
    override suspend fun plan(task: String, context: String): String = runInSandbox("openhands plan").stdout
    override suspend fun modify(plan: String): Boolean = runInSandbox("openhands modify").exitCode == 0
    override suspend fun build(): String = runInSandbox("make build").stdout
    override suspend fun test(): String = runInSandbox("make test").stdout
    override suspend fun review(diff: String): Boolean = runInSandbox("openhands review").exitCode == 0
    override suspend fun cancel(): Boolean = sandboxManager.destroySandbox(sandboxId)
}


class WorkerSelectorImpl(private val sandboxManager: RemoteSandboxManager) : WorkerSelector {
    override suspend fun selectBestWorker(
        requirements: TaskRequirements, 
        capabilityKnowledge: Map<String, CandidateEvaluation>
    ): CodingWorkerRuntime {
        // Task -> Capability requirements
        // -> Candidate workers
        val candidates = listOf("Aider", "MiniSWEAgent", "OpenHands")
        
        // -> Research/evidence scores
        var bestWorker = "Aider"
        var bestScore = -1
        
        for (candidate in candidates) {
            val eval = capabilityKnowledge[candidate]
            if (eval != null) {
                // -> Cost/performance
                val score = eval.effectivenessScore * eval.efficiencyScore - eval.integrationComplexity
                if (score > bestScore) {
                    bestScore = score
                    bestWorker = candidate
                }
            }
        }
        
        // -> Selected worker
        val sandboxId = "sandbox-unassigned" // Real impl provisions here or receives it
        return when (bestWorker) {
            "MiniSWEAgent" -> MiniSWEAgentRuntime(sandboxManager, sandboxId)
            "OpenHands" -> OpenHandsRuntime(sandboxManager, sandboxId)
            else -> AiderRuntime(sandboxManager, sandboxId)
        }
    }
}
