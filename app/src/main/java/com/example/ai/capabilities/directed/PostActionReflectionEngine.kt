package com.example.ai.capabilities.directed

data class NextStepAssessment(
    val lastEvent: String,
    val newStateOfReality: String,
    val unresolvedQuestions: List<String>,
    val emergingOpportunities: List<Opportunity>,
    val recommendedNextAction: Opportunity?,
    val reasoningSummary: String,
    val confidence: Float
)

interface PostActionReflectionEngine {
    fun evaluateStateChange(lastEvent: String, availableOpportunities: List<Opportunity>): NextStepAssessment
}

class PostActionReflectionEngineImpl : PostActionReflectionEngine {
    override fun evaluateStateChange(lastEvent: String, availableOpportunities: List<Opportunity>): NextStepAssessment {
        // 1. What just changed? (lastEvent)
        // 2. What does that unlock?
        val newState = "Event '$lastEvent' integrated. System capabilities potentially expanded. Reality landscape altered."
        
        // 3. What is now the weakest link?
        val unresolved = listOf(
            "Has the newly integrated architecture been tested end-to-end?",
            "Are recent epistemic assumptions still valid under the new state?",
            "Are there dormant opportunities that this new state unlocks?"
        )

        // 4. What is the highest-leverage next action?
        // In a real system, this delegates back to DirectedAgencyEngine.
        // We mock a highly relevant opportunity selection.
        val recommended = availableOpportunities.maxByOrNull { it.expectedLeverage * it.reversibility }

        return NextStepAssessment(
            lastEvent = lastEvent,
            newStateOfReality = newState,
            unresolvedQuestions = unresolved,
            emergingOpportunities = availableOpportunities,
            recommendedNextAction = recommended,
            reasoningSummary = if (recommended != null) {
                "High-value opportunity identified based on recent state change. Leveraging new capabilities."
            } else {
                "No available action exceeds the required leverage threshold. Deliberate observation is the highest-value decision."
            },
            confidence = 0.85f
        )
    }
}
