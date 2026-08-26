import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Replace import
content = content.replace("import com.example.github.MemoryManager", "import com.example.github.HierarchicalMemoryManager\nimport com.example.ai.CodeJarvis\nimport com.example.ai.CodingTools")

# Replace property
content = content.replace("val memoryManager = MemoryManager(context)", "val memoryManager = HierarchicalMemoryManager(context, memoryDao, embeddingEngine)\n    val codingTools = CodingTools(context)\n    val codeJarvis = CodeJarvis(codingTools, com.example.ai.TreeSitterEngine())")

# Replace RAG logic in sendMessage
old_rag_logic = """            var ragContext = ""
            try {
                val currentEmbedding = embeddingEngine.generateEmbedding(text)
                
                val allMemories = memoryDao.getAllFragments()
                val nearest = allMemories.mapNotNull { mem ->
                    if (mem.embedding.isBlank()) return@mapNotNull null
                    val emb = mem.embedding.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                    if (emb.size != currentEmbedding.size) return@mapNotNull null
                    var dotProduct = 0f
                    for (i in emb.indices) { dotProduct += emb[i] * currentEmbedding[i] }
                    mem to dotProduct
                }.sortedByDescending { it.second }.take(3).map { it.first }
                if (nearest.isNotEmpty()) {
                    ragContext = "\n\n[RETRIEVED MEMORIES]\n" + nearest.joinToString("\n") { (if(it.isUser) "User" else "Assistant") + ": " + it.text }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }"""
            
new_rag_logic = """            val ragContext = memoryManager.retrieveRelevantContext(text)"""

content = content.replace(old_rag_logic, new_rag_logic)

# Replace memoryManager.saveConversationLocal with saveEpisodicMemory
content = content.replace("memoryManager.saveConversationLocal", "memoryManager.saveEpisodicMemory")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
