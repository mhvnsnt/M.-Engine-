package com.example.github

import android.content.Context
import android.util.Log
import com.example.ai.EmbeddingEngine
import com.example.data.MemoryFragment
import com.example.data.MemoryFragmentDao
import com.example.data.MessageEntity
import com.example.network.GitHubApiService
import com.example.network.GitHubBlobRequest
import com.example.network.GitHubCreateCommitRequest
import com.example.network.GitHubTreeItemRequest
import com.example.network.GitHubTreeRequest
import com.example.network.GitHubUpdateRefRequest
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

class HierarchicalMemoryManager(
    private val context: Context,
    private val memoryDao: MemoryFragmentDao,
    private val embeddingEngine: EmbeddingEngine,
    private val apiService: GitHubApiService = RetrofitClient.githubService
) {
    private val owner = "mhvnsnt"
    private val repo = "M.-Engine"
    private val branch = "main"

    // 1. Episodic Memory (Raw events)
    suspend fun saveEpisodicMemory(sessionId: Long, messages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        try {
            val memoryDir = File(context.filesDir, "memory/sessions")
            if (!memoryDir.exists()) memoryDir.mkdirs()

            val fileName = "session_$sessionId.md"
            val file = File(memoryDir, fileName)
            
            val sb = java.lang.StringBuilder()
            sb.append("# Session $sessionId\n\n")
            messages.forEach { msg ->
                val role = if (msg.isUser) "User" else "M. Engine"
                sb.append("**$role:**\n${msg.text}\n\n")
            }
            
            file.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("HierarchicalMemory", "Error saving episodic memory locally", e)
        }
    }

    // 2. Hybrid RAG Retrieval (Dense + Sparse approximation)
    suspend fun retrieveRelevantContext(query: String, topK: Int = 3): String = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = embeddingEngine.generateEmbedding(query)
            val allMemories = memoryDao.getAllFragments()
            
            val queryWords = query.lowercase().split(Regex("\\s+")).toSet()

            val scoredMemories = allMemories.mapNotNull { mem ->
                if (mem.embedding.isBlank()) return@mapNotNull null
                
                // Dense Score (Cosine Similarity)
                val emb = mem.embedding.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                if (emb.size != queryEmbedding.size) return@mapNotNull null
                
                var dotProduct = 0f
                for (i in emb.indices) { dotProduct += emb[i] * queryEmbedding[i] }
                
                // Sparse Score (Jaccard approximation / Keyword overlap)
                val memWords = mem.text.lowercase().split(Regex("\\s+")).toSet()
                val overlap = queryWords.intersect(memWords).size
                val sparseScore = overlap.toFloat() / (queryWords.size.coerceAtLeast(1))
                
                // Hybrid Score (Weight: 70% Dense, 30% Sparse)
                val hybridScore = (dotProduct * 0.7f) + (sparseScore * 0.3f)
                
                mem to hybridScore
            }.sortedByDescending { it.second }.take(topK).map { it.first }

            if (scoredMemories.isNotEmpty()) {
                return@withContext "\n\n[RECALLED EPISODIC MEMORIES]\n" + scoredMemories.joinToString("\n---\n") { 
                    (if(it.isUser) "User: " else "M. Engine: ") + it.text 
                }
            }
        } catch (e: Exception) {
            Log.e("HierarchicalMemory", "Error in RAG retrieval", e)
        }
        return@withContext ""
    }

    // 3. System Prompt & Core Identity (Loaded on start)
    suspend fun getSystemPromptLocal(): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "memory/system-prompt.md")
            if (file.exists()) {
                file.readText()
            } else {
                context.assets.open("memory/system-prompt.md").bufferedReader().use { it.readText() }
            }
        } catch(e: Exception) {
            null
        }
    }

    suspend fun pullSystemPrompt(pat: String) = withContext(Dispatchers.IO) {
        try {
            val authHeader = if (pat.isNotEmpty()) "Bearer $pat" else null
            val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/memory/system-prompt.md"
            val response = apiService.downloadFile(url, authHeader)
            
            val memoryDir = File(context.filesDir, "memory")
            if (!memoryDir.exists()) memoryDir.mkdirs()
            File(memoryDir, "system-prompt.md").writeText(response.string())
            Log.d("HierarchicalMemory", "Successfully pulled system prompt from GitHub")
        } catch (e: Exception) {
            Log.e("HierarchicalMemory", "Failed to pull system prompt", e)
        }
    }

    // 4. Sync Episodic Memory to GitHub (Background)
    suspend fun syncSessionToGithub(pat: String, sessionId: Long, messages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        if (pat.isEmpty()) return@withContext
        try {
            val authHeader = "Bearer $pat"
            val sb = java.lang.StringBuilder()
            sb.append("# Session $sessionId\n\n")
            messages.forEach { msg ->
                val role = if (msg.isUser) "User" else "M. Engine"
                sb.append("**$role:**\n${msg.text}\n\n")
            }
            val content = sb.toString()
            val fileName = "memory/sessions/session_$sessionId.md"
            
            val refResponse = apiService.getReference(authHeader, owner, repo, branch)
            val baseSha = refResponse.objectInfo.sha
            val commitResponse = apiService.getCommit(authHeader, owner, repo, baseSha)
            val baseTreeSha = commitResponse.tree.sha
            
            val blobResponse = apiService.createBlob(authHeader, owner, repo, GitHubBlobRequest(content = content))
            val treeRequest = GitHubTreeRequest(
                base_tree = baseTreeSha,
                tree = listOf(GitHubTreeItemRequest(path = fileName, sha = blobResponse.sha))
            )
            val newTree = apiService.createTree(authHeader, owner, repo, treeRequest)
            
            val commitRequest = GitHubCreateCommitRequest(
                message = "Auto-commit: Sync episodic memory session $sessionId",
                tree = newTree.sha,
                parents = listOf(baseSha)
            )
            val newCommit = apiService.createCommit(authHeader, owner, repo, commitRequest)
            apiService.updateReference(authHeader, owner, repo, branch, GitHubUpdateRefRequest(sha = newCommit.sha))
            
            Log.d("HierarchicalMemory", "Successfully synced session $sessionId to GitHub")
        } catch (e: Exception) {
            Log.e("HierarchicalMemory", "Failed to sync to GitHub", e)
        }
    }
}
