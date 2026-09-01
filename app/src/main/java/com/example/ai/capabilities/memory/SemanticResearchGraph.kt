package com.example.ai.capabilities.memory

interface SemanticResearchGraph {
    fun addNode(artifact: PersistentResearchArtifact)
    fun linkNodes(parentId: String, childId: String, relationship: String)
    fun searchByRelevance(objectiveId: String, keywords: List<String>): List<PersistentResearchArtifact>
    fun getDescendants(artifactId: String): List<PersistentResearchArtifact>
}

class SemanticResearchGraphImpl : SemanticResearchGraph {
    private val nodes = mutableMapOf<String, PersistentResearchArtifact>()
    private val edges = mutableListOf<Edge>()

    data class Edge(val sourceId: String, val targetId: String, val relationship: String)

    override fun addNode(artifact: PersistentResearchArtifact) {
        nodes[artifact.artifactId] = artifact
    }

    override fun linkNodes(parentId: String, childId: String, relationship: String) {
        edges.add(Edge(parentId, childId, relationship))
    }

    override fun searchByRelevance(objectiveId: String, keywords: List<String>): List<PersistentResearchArtifact> {
        // Return dormant or active artifacts that match keywords or objective, but aren't SUPERSEDED
        return nodes.values.filter { artifact ->
            (artifact.objectiveId == objectiveId || keywords.any { k -> artifact.claims.any { it.contains(k, ignoreCase = true) } }) &&
            artifact.status != ArtifactStatus.SUPERSEDED
        }
    }

    override fun getDescendants(artifactId: String): List<PersistentResearchArtifact> {
        val descendants = mutableListOf<PersistentResearchArtifact>()
        val queue = mutableListOf(artifactId)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            val children = edges.filter { it.sourceId == currentId }.map { it.targetId }
            for (childId in children) {
                nodes[childId]?.let { 
                    descendants.add(it)
                    queue.add(childId)
                }
            }
        }
        return descendants
    }
}
