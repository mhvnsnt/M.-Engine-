package com.example.ai.capabilities.tandem

enum class AutonomyGradient(val level: Int) {
    OBSERVE(0),             // Research only
    PROPOSE(1),             // Research + generate possible improvements
    EXPERIMENT(2),          // Create isolated branches/sandboxes and test ideas
    IMPLEMENT(3),           // Modify its own development branch
    INTEGRATE(4),           // Run verification and prepare a merge proposal
    OWNER_APPROVAL(5),      // Human authorizes integration into the primary system
    DELEGATED_AUTONOMY(6)   // Pre-authorized classes of changes integrate automatically
}

enum class SignalType {
    NEW_REQUIREMENT,
    NEW_PREFERENCE,
    NEW_ONTOLOGY,
    CORRECTION,
    ARCHITECTURE_IMPROVEMENT
}

data class DevelopmentSignal(
    val type: SignalType,
    val description: String,
    val priority: Double, // 0.0 to 1.0
    val detectedAt: Long = System.currentTimeMillis()
)

data class MindstreamEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val mission: String,
    val currentState: String,
    val objective: String,
    val whyThisMatters: String,
    val currentAction: String,
    val evidence: String? = null,
    val experimentResult: String? = null,
    val decision: String? = null,
    val nextAction: String? = null,
    val learning: String? = null
)
