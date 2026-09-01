package com.example.ai.capabilities.directed

data class GoalTension(
    val objectiveA: String,
    val objectiveB: String,
    val conflictDescription: String,
    val availableStrategies: List<String>,
    val selectedStrategy: String,
    val reasoning: String
)

interface GoalTensionEngine {
    fun detectTensions(activeObjectives: List<ObjectiveNode>): List<GoalTension>
    fun resolveTension(tension: GoalTension): GoalTension
}

class GoalTensionEngineImpl : GoalTensionEngine {
    override fun detectTensions(activeObjectives: List<ObjectiveNode>): List<GoalTension> {
        val tensions = mutableListOf<GoalTension>()
        // Mock detection: if there is an objective for "Maximum autonomy" and "Owner awareness", create tension
        val hasAutonomy = activeObjectives.any { it.name.contains("autonomy", ignoreCase = true) }
        val hasAwareness = activeObjectives.any { it.name.contains("awareness", ignoreCase = true) }

        if (hasAutonomy && hasAwareness) {
            tensions.add(
                GoalTension(
                    objectiveA = "Increase autonomous experimentation",
                    objectiveB = "Preserve owner review of significant changes",
                    conflictDescription = "Autonomous implementation may reduce direct owner awareness.",
                    availableStrategies = listOf(
                        "Experiment in sandbox",
                        "Prepare proposal",
                        "Implement on feature branch",
                        "Pause for owner approval"
                    ),
                    selectedStrategy = "",
                    reasoning = ""
                )
            )
        }
        return tensions
    }

    override fun resolveTension(tension: GoalTension): GoalTension {
        // Resolve tension dynamically, e.g., picking the most reversible strategy
        return tension.copy(
            selectedStrategy = "Experiment in sandbox",
            reasoning = "High reversibility, moderate information gain, no irreversible owner impact."
        )
    }
}
