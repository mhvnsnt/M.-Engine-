package com.example.ai.capabilities

import java.util.UUID

enum class MemoryProvenance {
    EXPLICIT,  // Things the user directly taught M. Engine
    OBSERVED,  // Things derived directly from user behavior without inference
    INFERRED,  // Patterns M. Engine thinks might be true
    CONFIRMED, // Inferences the user has subsequently validated
    REJECTED   // Things M. Engine learned were wrong because the user corrected/rejected them
}

data class UserInteraction(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val context: String,
    val actionTaken: String,
    val userFeedback: String? = null
)

data class PatternHypothesis(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val confidence: Float,
    val supportingInteractions: List<String>
)

data class MemoryProposal(
    val id: String = UUID.randomUUID().toString(),
    val memoryCategory: String, // e.g., "Design Taste", "Coding Style", "Workflow"
    val content: String,
    val provenance: MemoryProvenance
)

data class UserTeaching(
    val explicitInstruction: String,
    val context: String? = null
)

/**
 * The Personal Evolution Engine.
 * Responsible for learning the user's Identity, Goals, Preferences, and Workflows with strict provenance.
 * Models the "Personal Self" side of M. Engine's dual architecture.
 */
interface PersonalEvolutionEngine {
    
    suspend fun observe(event: UserInteraction)
    
    suspend fun inferPatterns(history: List<UserInteraction>): List<PatternHypothesis>
    
    suspend fun proposeMemoryUpdate(hypothesis: PatternHypothesis): MemoryProposal
    
    suspend fun learn(teaching: UserTeaching)
    
    suspend fun adapt(approvedChange: MemoryProposal)
    
    suspend fun explain(memoryId: String): MemoryProposal?
}
