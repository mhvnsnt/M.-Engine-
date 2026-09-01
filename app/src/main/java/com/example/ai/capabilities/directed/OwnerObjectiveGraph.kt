package com.example.ai.capabilities.directed

enum class ObjectiveSignal {
    DIRECT_COMMAND,
    STRONG_PREFERENCE,
    EMERGING_INTEREST,
    SPECULATIVE_IDEA,
    BACKGROUND_HYPOTHESIS
}

data class ObjectiveNode(
    val id: String,
    val name: String,
    val category: String, // Personal, M. Engine, Bannon, Economic, etc.
    val signal: ObjectiveSignal,
    var weight: Double,
    val relatedNodes: MutableList<String> = mutableListOf()
)

interface OwnerObjectiveGraph {
    fun addNode(node: ObjectiveNode)
    fun updateNode(id: String, weight: Double, signal: ObjectiveSignal)
    fun linkNodes(sourceId: String, targetId: String)
    fun getRelevantObjectives(contextKeywords: List<String>): List<ObjectiveNode>
    fun getAllObjectives(): List<ObjectiveNode>
}

class OwnerObjectiveGraphImpl : OwnerObjectiveGraph {
    private val nodes = mutableMapOf<String, ObjectiveNode>()

    override fun addNode(node: ObjectiveNode) {
        nodes[node.id] = node
    }

    override fun updateNode(id: String, weight: Double, signal: ObjectiveSignal) {
        nodes[id]?.let {
            it.weight = weight
            // Using a copy or just mutating if it were var, we'll mutate since it's an object with vars in a real impl
            // But we made signal val, let's just replace the node
            nodes[id] = it.copy(weight = weight, signal = signal)
        }
    }

    override fun linkNodes(sourceId: String, targetId: String) {
        nodes[sourceId]?.relatedNodes?.add(targetId)
        nodes[targetId]?.relatedNodes?.add(sourceId)
    }

    override fun getRelevantObjectives(contextKeywords: List<String>): List<ObjectiveNode> {
        return nodes.values.filter { node ->
            contextKeywords.any { keyword -> 
                node.name.contains(keyword, ignoreCase = true) || node.category.contains(keyword, ignoreCase = true)
            }
        }.sortedByDescending { it.weight }
    }

    override fun getAllObjectives(): List<ObjectiveNode> = nodes.values.toList()
}
