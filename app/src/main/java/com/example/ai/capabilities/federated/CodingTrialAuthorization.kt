package com.example.ai.capabilities.federated

enum class TrialState {
    AUTHORIZED,
    REPOSITORY_OBSERVED,
    WORKSPACE_CREATED,
    WORKER_REACHED,
    JOB_DISPATCHED,
    WORKER_REPORTED_RESULT,
    ARTIFACTS_INDEPENDENTLY_INSPECTED,
    TEST_EXECUTION_OBSERVED,
    CLEANUP_INSPECTED,
    VERIFIED_PARTIAL,
    CAPABILITY_GAP,
    WORKER_UNREACHABLE,
    AUTHORIZATION_FAILED,
    ARTIFACT_VERIFICATION_FAILED,
    TEST_FAILED,
    CLEANUP_UNKNOWN
}

enum class EpistemicCapabilityState {
    IMPLEMENTED_UNVERIFIED,
    ORCHESTRATOR_LOGIC_VERIFIED,
    PARTIALLY_VERIFIED,
    VERIFIED_OPERATIONAL
}

data class TrialPermissions(
    val readRepository: Boolean = true,
    val modifyWorkspace: Boolean = true,
    val pushToProduction: Boolean = false,
    val mergePullRequest: Boolean = false,
    val deleteRepositoryContent: Boolean = false,
    val modifyCredentials: Boolean = false
)

data class TrialTask(
    val type: String = "SMALL_REVERSIBLE_REALITY_TRIAL",
    val maxFilesChanged: Int = 3,
    val maxExecutionAttempts: Int = 2,
    val instruction: String
)

data class VerificationRequirements(
    val requireStartingCommitObservation: Boolean = true,
    val requirePostChangeDiff: Boolean = true,
    val requireSha256DiffHash: Boolean = true,
    val requireIndependentTestArtifacts: Boolean = true,
    val requireExitCodes: Boolean = true,
    val requireWorkspaceCleanupCheck: Boolean = true
)

data class BudgetConfig(
    val wallClockTimeoutMs: Long = 1200000,
    val workerAttempts: Int = 1
)

data class CodingTrialAuthorization(
    val authority: String = "BOUNDED_AUTOMATION",
    val repositorySource: String = "AUTHORIZED_GITHUB_REPOSITORY",
    val repositoryId: String,
    val startingCommit: String = "RESOLVE_AT_TRIAL_START",
    val workspaceType: String = "EPHEMERAL_ISOLATED_WORKSPACE",
    val workspacePersistence: String = "NONE",
    val permissions: TrialPermissions = TrialPermissions(),
    val allowedOperations: List<String> = listOf(
        "inspect", 
        "create_or_modify_authorized_files", 
        "run_approved_build_commands", 
        "run_approved_test_commands", 
        "generate_diff"
    ),
    val task: TrialTask,
    val verification: VerificationRequirements = VerificationRequirements(),
    val networkPolicy: String = "minimum_required_only",
    val budget: BudgetConfig = BudgetConfig()
)

data class LiveCodingTrialEvidence(
    val authorization: CodingTrialAuthorization,
    val resolvedCommitSha: String?,
    val stateProgression: List<TrialState>,
    val finalState: TrialState,
    val capabilityState: EpistemicCapabilityState,
    val diffHash: String? = null,
    val artifacts: Map<String, String> = emptyMap()
) {
    fun printEvidenceLedger() {
        println("━━━━━━━━ M. ENGINE — LIVE CODING REALITY TRIAL ━━━━━━━━")
        println("AUTHORIZATION: ${authorization.authority}")
        println("REPOSITORY: ${authorization.repositoryId}")
        println("RESOLVED COMMIT: ${resolvedCommitSha ?: "UNKNOWN"}")
        println("\nSTATE PROGRESSION:")
        stateProgression.forEachIndexed { index, state -> 
            println("  ${index + 1}. $state")
        }
        println("\nFINAL STATE: $finalState")
        println("CAPABILITY STATE: $capabilityState")
        if (diffHash != null) {
            println("DIFF HASH: $diffHash")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
