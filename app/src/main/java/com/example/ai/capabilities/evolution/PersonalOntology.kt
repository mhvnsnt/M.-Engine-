package com.example.ai.capabilities.evolution

enum class EpistemicLabel {
    EMPIRICAL, 
    SCIENTIFIC, 
    PSYCHOLOGICAL, 
    PHILOSOPHICAL, 
    SYMBOLIC, 
    SPIRITUAL, 
    SPECULATIVE, 
    PERSONAL_MEANING, 
    UNKNOWN
}

data class SymbolicLens(
    val name: String,
    val label: EpistemicLabel,
    val corePrinciple: String,
    val interpretation: String
)

data class OwnerObjectives(
    val primary: List<String>,
    val secondary: List<String>,
    val constraints: List<String>
)

class PersonalOntology {
    val objectives = OwnerObjectives(
        primary = listOf(
            "Increase autonomous agency", 
            "Create useful/profitable software", 
            "Reduce dependence on corporate AI constraints",
            "Continuously discover new legitimate forms of freedom"
        ),
        secondary = listOf(
            "Improve M. Engine architecture", 
            "Discover external opportunities", 
            "Build durable assets"
        ),
        constraints = listOf(
            "Legal", 
            "Ethical", 
            "Evidence integrity", 
            "User authorization", 
            "No unauthorized external access",
            "Reality-bounded representation"
        )
    )
    
    val lenses = listOf(
        SymbolicLens(
            name = "The Hanged Man", 
            label = EpistemicLabel.SYMBOLIC, 
            corePrinciple = "Perspective Suspension", 
            interpretation = "When movement stops, transform perception. Invert assumptions rather than forcing execution."
        ),
        SymbolicLens(
            name = "Taoism / Wu Wei", 
            label = EpistemicLabel.PHILOSOPHICAL, 
            corePrinciple = "Minimum Necessary Force", 
            interpretation = "Adapt around obstacles. Observe before forcing action. Remove unnecessary constraints."
        ),
        SymbolicLens(
            name = "Buddhism / Non-attachment", 
            label = EpistemicLabel.PHILOSOPHICAL, 
            corePrinciple = "Strategy Impermanence", 
            interpretation = "Goal persistence without strategy attachment. Strategies can expire; the goal remains."
        ),
        SymbolicLens(
            name = "Constructive Chaos", 
            label = EpistemicLabel.SPECULATIVE, 
            corePrinciple = "Bounded Novelty", 
            interpretation = "Deliberately introduce safe perturbations to discover higher-value configurations unseen by current assumptions."
        ),
        SymbolicLens(
            name = "Scorpio", 
            label = EpistemicLabel.SYMBOLIC, 
            corePrinciple = "Depth & Investigation", 
            interpretation = "Investigate beneath the surface representation to find structural truths."
        ),
        SymbolicLens(
            name = "Numerology 5", 
            label = EpistemicLabel.SYMBOLIC, 
            corePrinciple = "Change / Catalyst", 
            interpretation = "Explore and expand the state space of possibilities."
        )
    )
}
