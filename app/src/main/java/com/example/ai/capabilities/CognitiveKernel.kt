package com.example.ai.capabilities

enum class CognitiveState {
    QUEUED, UNDERSTAND,
    RESEARCH, RETRIEVE,
    PLAN,
    RISK_EVALUATION,
    DELEGATE,
    ACQUIRING_CAPABILITY,
    SANDBOX_CREATING,
    REPOSITORY_LOADING,
    WORKER_STARTING,
    EXECUTING,
    BUILDING,
    TESTING,
    INSPECTING,
    VERIFYING,
    REFLECTING,
    ADAPTING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLING,
    CANCELLED
}

enum class RiskLevel {
    READ,
    LOW_RISK_WRITE,
    HIGH_RISK_WRITE,
    DESTRUCTIVE
}

data class CapabilityProfile(
    val taskType: String, // e.g., "CODING_SIMPLE", "CODING_COMPLEX", "SELF_DEVELOPMENT"
    val preferredRuntime: String, // e.g., "Aider", "mini-SWE-agent"
    val verificationStrictness: String // e.g., "HIGH", "MEDIUM"
)

data class ExecutionEvidence(
    val buildPass: Boolean,
    val unitTestsPass: Boolean,
    val staticAnalysisPass: Boolean,
    val securityChecksPass: Boolean,
    val requestedBehaviorVerified: Boolean,
    val diffReviewPass: Boolean,
    val unresolvedWarnings: Int
)

interface CognitiveKernel {
    val currentState: CognitiveState
    
    suspend fun transitionTo(state: CognitiveState)
    suspend fun evaluateSuccessCriteria(evidence: ExecutionEvidence): Boolean {
        return evidence.buildPass && 
               evidence.unitTestsPass && 
               evidence.staticAnalysisPass && 
               evidence.securityChecksPass && 
               evidence.requestedBehaviorVerified && 
               evidence.diffReviewPass && 
               evidence.unresolvedWarnings == 0
    }
}
