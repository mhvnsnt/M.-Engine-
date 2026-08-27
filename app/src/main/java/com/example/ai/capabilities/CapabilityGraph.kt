package com.example.ai.capabilities

enum class CapabilityDomain {
    AI, AGENTS, CODING, REALITY, MEMORY, INFRASTRUCTURE, UNKNOWN
}

enum class HarvestIntegrationMode {
    ARCHITECTURAL_REFERENCE,
    NATIVE_KOTLIN_ADAPTATION,
    DIRECT_LIBRARY_INTEGRATION,
    MODEL_ONLY,
    REMOTE_SERVICE,
    REJECT
}

data class ImplementationDetails(
    val repoUrl: String,
    val filePath: String,
    val type: String, // Kotlin, Python, JS, etc.
    val dependencies: List<String>,
    val testCoverage: Double,
    val maturityScore: Double,
    val evidenceLedgerId: String?
)

data class CapabilityNode(
    val id: String,
    val name: String,
    val domain: CapabilityDomain,
    val description: String,
    val implementations: List<ImplementationDetails>
)

data class HarvestProvenance(
    val capabilityId: String,
    val currentImplementation: String?,
    val candidateName: String,
    val sourceUrl: String,
    val license: String,
    val versionOrCommit: String,
    val selectionReason: String,
    val benchmarkScore: Double,
    val evidenceId: String,
    val integrationMode: HarvestIntegrationMode,
    val lastEvaluatedAt: Long,
    val replacementTarget: String?
)

interface CapabilityGraphDatabase {
    suspend fun insertNode(node: CapabilityNode)
    suspend fun getNodesByDomain(domain: CapabilityDomain): List<CapabilityNode>
    suspend fun recordProvenance(provenance: HarvestProvenance)
    suspend fun findDuplicates(): List<CapabilityNode>
    suspend fun getMissingCapabilities(): List<String>
}

class CapabilityGraphDatabaseImpl : CapabilityGraphDatabase {
    private val nodes = mutableListOf<CapabilityNode>()
    private val provenanceLog = mutableListOf<HarvestProvenance>()

    override suspend fun insertNode(node: CapabilityNode) {
        val existing = nodes.find { it.id == node.id }
        if (existing != null) {
            nodes.remove(existing)
            nodes.add(existing.copy(implementations = existing.implementations + node.implementations))
        } else {
            nodes.add(node)
        }
    }

    override suspend fun getNodesByDomain(domain: CapabilityDomain): List<CapabilityNode> {
        return nodes.filter { it.domain == domain }
    }

    override suspend fun recordProvenance(provenance: HarvestProvenance) {
        provenanceLog.add(provenance)
    }

    override suspend fun findDuplicates(): List<CapabilityNode> {
        // Return nodes that have more than 1 implementation across repositories
        return nodes.filter { it.implementations.size > 1 }
    }

    override suspend fun getMissingCapabilities(): List<String> {
        return listOf("advanced_video_analysis", "autonomous_repo_crawling", "hyper_parameter_tuning")
    }
}
