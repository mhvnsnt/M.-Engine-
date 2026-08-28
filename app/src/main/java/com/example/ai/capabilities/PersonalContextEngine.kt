package com.example.ai.capabilities

enum class MemoryCategory {
    EXPLICIT,    // Things directly told by user
    OBSERVED,    // Patterns observed by the engine
    CONFIRMED,   // Hypotheses the user has validated
    INFERRED,    // Hypotheses the engine has about the user
    REJECTED,    // Things the user has corrected
    PROJECT,     // Knowledge about specific games/apps/repos
    HISTORY,     // Dev history: bugs, fixes, regressions, decisions
    PREFERENCE,  // How the user wants the engine to work
    GOAL         // What the user is trying to build and why
}

data class ContextMemory(
    val id: String,
    val category: MemoryCategory,
    val content: String,
    val timestamp: Long,
    val relatedEntities: List<String> = emptyList() // Repos, projects, etc.
)

interface PersonalContextEngine {
    suspend fun storeMemory(category: MemoryCategory, content: String, entities: List<String> = emptyList())
    suspend fun retrieveContext(query: String, categories: List<MemoryCategory>? = null): List<ContextMemory>
    suspend fun applyCorrection(originalMemoryId: String, correction: String)
    suspend fun getOperatingRules(): List<ContextMemory>
    suspend fun getProjectKnowledge(projectId: String): List<ContextMemory>
}
