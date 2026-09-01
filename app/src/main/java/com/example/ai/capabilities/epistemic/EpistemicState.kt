package com.example.ai.capabilities.epistemic

enum class EpistemicStatus {
    EMPIRICAL,      
    ESTABLISHED,    
    SUPPORTED,      
    EMERGING,       
    CONTESTED,      
    SPECULATIVE,    
    SYMBOLIC,       
    EXPIRED         
}

enum class SourceRole {
    PRIMARY_EVIDENCE,
    REPLICATION,
    INDEPENDENT_CORROBORATION,
    EXPERT_SYNTHESIS,
    SECONDARY_REPORTING,
    COMMENTARY,
    SYMBOLIC_INTERPRETATION
}

data class KnowledgeSource(
    val uri: String,
    val role: SourceRole,
    val dateObserved: Long
)

data class KnowledgeClaim(
    val id: String,
    val statement: String,
    val domain: String,
    val sources: List<KnowledgeSource>,
    val discoveryDate: Long,
    var lastVerified: Long,
    var nextReview: Long,
    var confidence: Double,
    var status: EpistemicStatus,
    var supersededBy: String? = null,
    var contradicts: List<String> = emptyList(),
    
    // Mission 10: Adversarial Inquiry & Reality Maintenance
    val falsificationCondition: String? = null,
    
    // Priority metrics (0.0 to 1.0)
    var relevance: Double = 0.5,
    var changeLikelihood: Double = 0.5,
    var consequence: Double = 0.5,
    var verificationCost: Double = 0.5 // Cannot be 0
) {
    val uncertainty: Double
        get() = 1.0 - confidence
        
    val epistemicPriority: Double
        get() = (relevance * changeLikelihood * consequence * uncertainty) / (verificationCost.coerceAtLeast(0.01))
}

data class KnowledgeConflict(
    val id: String,
    val claimAId: String,
    val claimBId: String,
    val evidenceA: List<KnowledgeSource>,
    val evidenceB: List<KnowledgeSource>,
    val discoveryDate: Long,
    var resolutionStatus: String 
)

data class BeliefRevision(
    val claimId: String,
    val previousStatement: String,
    val newStatement: String,
    val previousConfidence: Double,
    val newConfidence: Double,
    val revisionDate: Long,
    val reason: String,
    val learnedLesson: String,
    
    // Mission 10: Intellectual History
    val failedAssumption: String? = null,
    val monitoringDirective: String? = null
)
