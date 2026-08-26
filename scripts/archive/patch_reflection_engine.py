import re

with open('app/src/main/java/com/example/ai/ReflectionEngine.kt', 'r') as f:
    content = f.read()

target = """class ReflectionEngine(
    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine
) {"""

replacement = """class ReflectionEngine(
    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine,
    private val locationRepository: com.example.data.LocationRepository? = null
) {"""
content = content.replace(target, replacement)

target2 = """    private suspend fun reflectOnSessions() {
        val archival = memoryDao.getAllFragments()
        if (archival.size > 10) {
            // Simplified logic: periodically summarize into CORE memory
            val summary = "User demonstrates preference for local tooling (Tree-sitter, JGit) and performance (Llama.cpp on mobile GPU). Expects offline-first reasoning and semantic AST awareness."
            val embedding = embeddingEngine.generateEmbedding(summary)
            memoryDao.insert(
                MemoryFragment(
                    text = summary,
                    timestamp = System.currentTimeMillis(),
                    isUser = false,
                    embedding = embedding.joinToString(","),
                    type = "CORE"
                )
            )
            Log.d("ReflectionEngine", "Inserted CORE reflection fragment.")
        }
    }"""

replacement2 = """    private suspend fun reflectOnSessions() {
        val archival = memoryDao.getAllFragments()
        if (archival.size > 10) {
            // Check for location correction updates
            locationRepository?.let { repo ->
                val region = repo.fetchCurrentLocationAndRegion()
                // Pseudo logic: analyze text to see if there are region-specific facts
                if (region != null) {
                    Log.d("ReflectionEngine", "Extracted local notes for ${region.displayName}")
                }
            }
        
            // Simplified logic: periodically summarize into CORE memory
            val summary = "User demonstrates preference for local tooling (Tree-sitter, JGit) and performance (Llama.cpp on mobile GPU). Expects offline-first reasoning and semantic AST awareness."
            val embedding = embeddingEngine.generateEmbedding(summary)
            memoryDao.insert(
                MemoryFragment(
                    text = summary,
                    timestamp = System.currentTimeMillis(),
                    isUser = false,
                    embedding = embedding.joinToString(","),
                    type = "CORE"
                )
            )
            Log.d("ReflectionEngine", "Inserted CORE reflection fragment.")
        }
    }"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ai/ReflectionEngine.kt', 'w') as f:
    f.write(content)

