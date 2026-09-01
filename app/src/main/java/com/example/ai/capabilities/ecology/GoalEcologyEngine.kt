package com.example.ai.capabilities.ecology

enum class EcologyCategory {
    BANNON,
    M_ENGINE,
    APPS,
    FUTURE_IDEAS,
    PERSONAL,
    ECONOMIC
}

data class EcologyNode(
    val id: String,
    val description: String,
    val category: EcologyCategory,
    val activeOpportunities: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val contradictions: List<String> = emptyList()
)

interface GoalEcologyEngine {
    fun registerNode(node: EcologyNode)
    fun identifyCrossProjectOpportunities(): List<String>
    fun getEcology(): List<EcologyNode>
    fun evolveStrategy(nodeId: String, newOpportunity: String, failedStrategy: String? = null)
}

class GoalEcologyEngineImpl : GoalEcologyEngine {
    private val nodes = mutableMapOf<String, EcologyNode>()

    override fun registerNode(node: EcologyNode) {
        nodes[node.id] = node
    }

    override fun identifyCrossProjectOpportunities(): List<String> {
        // In reality, this would search for patterns across nodes (e.g. Bannon animation + M. Engine multimodal research)
        return listOf("Opportunity: Use M. Engine multimodal research to resolve Bannon grappling state mechanics")
    }

    override fun getEcology(): List<EcologyNode> = nodes.values.toList()

    override fun evolveStrategy(nodeId: String, newOpportunity: String, failedStrategy: String?) {
        nodes[nodeId]?.let { currentNode ->
            val updatedOpportunities = currentNode.activeOpportunities.toMutableList()
            failedStrategy?.let { updatedOpportunities.remove(it) }
            if (!updatedOpportunities.contains(newOpportunity)) {
                updatedOpportunities.add(newOpportunity)
            }
            nodes[nodeId] = currentNode.copy(activeOpportunities = updatedOpportunities)
        }
    }
}
