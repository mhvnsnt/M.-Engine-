import re

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "r") as f:
    content = f.read()

target = "class CodeJarvis("
new = """import com.example.data.GraphNode
import com.example.data.GraphNodeDao

class CodeJarvis("""
content = content.replace(target, new)

target2 = "    private val treeSitterEngine: TreeSitterEngine"
new2 = """    private val treeSitterEngine: TreeSitterEngine,
    private val graphDao: GraphNodeDao"""
content = content.replace(target2, new2)

target3 = """        if (githubPat.isEmpty()) {
            return@withContext "Error: GitHub PAT is required for coding capabilities."
        }"""
new3 = """        if (githubPat.isEmpty()) {
            return@withContext "Error: GitHub PAT is required for coding capabilities."
        }
        
        // Mem0 Episodic Deduplication Logic
        val recentEpisodes = graphDao.getActiveNodesByType("EPISODIC")
        if (recentEpisodes.size > 5) {
            // Compress overlapping commands into a clean timeline summary
            val summaryText = "User executed ${recentEpisodes.size} commands today focusing on ${recentEpisodes.map { it.obj }.distinct().take(2).joinToString()}. Compressed."
            recentEpisodes.forEach { graphDao.invalidateNode(it.id) }
            graphDao.insert(GraphNode(
                subject = "User",
                predicate = "summarized_activity",
                obj = summaryText,
                type = "ARCHIVAL"
            ))
            Log.d("CodeJarvis", "Mem0: Compressed overlapping commands into ARCHIVAL timeline summary.")
        }
        
        // Log current episode
        graphDao.insert(GraphNode(
            subject = "User",
            predicate = "ran_command",
            obj = command.take(50),
            type = "EPISODIC"
        ))
"""
content = content.replace(target3, new3)

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "w") as f:
    f.write(content)
