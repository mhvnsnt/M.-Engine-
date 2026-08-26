import re

with open("app/src/main/java/com/example/ai/ReflectionEngine.kt", "r") as f:
    content = f.read()

target1 = "class ReflectionEngine("
new1 = """import com.example.data.GraphNode
import com.example.data.GraphNodeDao

class ReflectionEngine("""
content = content.replace(target1, new1)

target2 = "    private val memoryDao: MemoryFragmentDao,"
new2 = """    private val memoryDao: MemoryFragmentDao,
    private val graphDao: GraphNodeDao,"""
content = content.replace(target2, new2)

target3 = "            val summary = \"User demonstrates preference for local tooling (Tree-sitter, JGit) and performance (Llama.cpp on mobile GPU). Expects offline-first reasoning and semantic AST awareness.\""
new3 = """            // Letta / Cognee Graph memory logic
            val summary = "User demonstrates preference for local tooling (Tree-sitter, JGit) and performance (Llama.cpp on mobile GPU). Expects offline-first reasoning and semantic AST awareness."
            
            val node = GraphNode(
                subject = "User",
                predicate = "prefers",
                obj = "local tooling and offline-first reasoning",
                type = "CORE"
            )
            graphDao.insert(node)
"""
content = content.replace(target3, new3)

with open("app/src/main/java/com/example/ai/ReflectionEngine.kt", "w") as f:
    f.write(content)
