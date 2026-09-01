package com.example.ai.capabilities.directed

import com.example.ai.capabilities.boundary.AutonomyLevel
import com.example.ai.capabilities.ecology.GoalEcologyEngine

interface AutonomousOpportunityLoop {
    fun onNoActiveMission(lastEvent: String, currentAutonomyLevel: AutonomyLevel): String
}

class AutonomousOpportunityLoopImpl(
    private val reflectionEngine: PostActionReflectionEngine,
    private val directedAgency: DirectedAgencyEngine,
    private val goalEcology: GoalEcologyEngine
) : AutonomousOpportunityLoop {

    override fun onNoActiveMission(lastEvent: String, currentAutonomyLevel: AutonomyLevel): String {
        // ENFORCE INVARIANT: Empty Queue ≠ Idle Purpose
        
        // 1. Gather opportunities (mocked cross-project generation)
        val opportunities = listOf(
            Opportunity(
                id = "opp-${System.currentTimeMillis()}-1",
                description = "Integration audit of newly added subsystems",
                alignedObjectives = listOf("M. Engine Reliability", "Directed Autonomous Agency"),
                expectedLeverage = 0.9, reversibility = 1.0, evidenceConfidence = 0.8,
                cost = 0.2, risk = 0.1, attentionConsumption = 0.1
            ),
            Opportunity(
                id = "opp-${System.currentTimeMillis()}-2",
                description = "Knowledge freshness scan of upstream dependencies",
                alignedObjectives = listOf("M. Engine Reliability"),
                expectedLeverage = 0.6, reversibility = 1.0, evidenceConfidence = 0.9,
                cost = 0.3, risk = 0.05, attentionConsumption = 0.2
            )
        )

        // 2. Reflect on recent events
        val assessment = reflectionEngine.evaluateStateChange(lastEvent, opportunities)
        val action = assessment.recommendedNextAction

        // 3. Format the Mindstream / Observatory Report
        val sb = StringBuilder()
        sb.appendLine("━━━━━━━━ M. ENGINE — DIRECTED INITIATIVE ━━━━━━━━")
        sb.appendLine()
        sb.appendLine("CURRENT ORIENTATION")
        sb.appendLine("Primary objective: Active")
        sb.appendLine()
        sb.appendLine("LAST EVENT")
        sb.appendLine(assessment.lastEvent)
        sb.appendLine()
        sb.appendLine("AUTONOMOUS INTERPRETATION")
        sb.appendLine(assessment.newStateOfReality)
        sb.appendLine()
        sb.appendLine("UNRESOLVED QUESTIONS")
        assessment.unresolvedQuestions.forEach { sb.appendLine("• $it") }
        sb.appendLine()

        if (action != null) {
            sb.appendLine("ACTIVE BACKGROUND VECTOR")
            sb.appendLine(action.description)
            sb.appendLine("Reasoning: ${assessment.reasoningSummary}")
        } else {
            sb.appendLine("ACTIVE BACKGROUND VECTOR")
            sb.appendLine("Deliberate Observation")
            sb.appendLine("Reasoning: ${assessment.reasoningSummary}")
        }
        
        sb.appendLine()
        sb.appendLine("AUTONOMY LEVEL")
        sb.appendLine(currentAutonomyLevel.name.replace("_", " "))
        sb.appendLine()
        sb.appendLine("NEXT DECISION TRIGGER")
        sb.appendLine("• experiment completion")
        sb.appendLine("• new owner directive")
        sb.appendLine("• relevant external information change")
        sb.appendLine("• scheduled epistemic review")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return sb.toString().trimEnd()
    }
}
