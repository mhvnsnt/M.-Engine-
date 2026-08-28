package com.example.ai.capabilities

class PersonalContextEngineImpl : PersonalContextEngine {
    private val memories = mutableListOf<ContextMemory>()

    init {
        // Seed core AGENTS.md explicit operational rules
        memories.add(
            ContextMemory(
                id = "rule-reality-contract",
                category = MemoryCategory.EXPLICIT,
                content = "Strictly adhere to REALITY_CONTRACT.md. Never declare completion without physical build/run verification. Zero simulated/fake evidence.",
                timestamp = System.currentTimeMillis(),
                relatedEntities = listOf("global", "mhvnsnt/M.-Engine-")
            )
        )
        memories.add(
            ContextMemory(
                id = "rule-provider-independence",
                category = MemoryCategory.PREFERENCE,
                content = "Provider Independence: Intelligence is interchangeable. M. Engine control plane persists missions across upstream vendor failures.",
                timestamp = System.currentTimeMillis(),
                relatedEntities = listOf("routing", "resilience")
            )
        )
    }

    override suspend fun storeMemory(
        category: MemoryCategory,
        content: String,
        entities: List<String>
    ) {
        val memory = ContextMemory(
            id = "mem-${System.currentTimeMillis()}-${memories.size}",
            category = category,
            content = content,
            timestamp = System.currentTimeMillis(),
            relatedEntities = entities
        )
        memories.add(memory)
    }

    override suspend fun retrieveContext(
        query: String,
        categories: List<MemoryCategory>?
    ): List<ContextMemory> {
        val filtered = if (categories != null) {
            memories.filter { it.category in categories }
        } else {
            memories
        }
        val lowerQuery = query.lowercase()
        return filtered.filter { mem ->
            mem.content.lowercase().contains(lowerQuery) ||
            mem.relatedEntities.any { it.lowercase().contains(lowerQuery) } ||
            query.isBlank()
        }
    }

    override suspend fun applyCorrection(originalMemoryId: String, correction: String) {
        val idx = memories.indexOfFirst { it.id == originalMemoryId }
        if (idx >= 0) {
            val old = memories[idx]
            memories[idx] = old.copy(
                category = MemoryCategory.REJECTED,
                content = "REJECTED: ${old.content} -> CORRECTION: $correction"
            )
            storeMemory(
                category = MemoryCategory.EXPLICIT,
                content = correction,
                entities = old.relatedEntities
            )
        }
    }

    override suspend fun getOperatingRules(): List<ContextMemory> {
        return memories.filter { it.category == MemoryCategory.EXPLICIT || it.category == MemoryCategory.PREFERENCE }
    }

    override suspend fun getProjectKnowledge(projectId: String): List<ContextMemory> {
        return memories.filter { it.relatedEntities.contains(projectId) }
    }
}
