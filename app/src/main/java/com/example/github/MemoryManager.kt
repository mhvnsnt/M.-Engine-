package com.example.github

import android.content.Context
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoryManager(private val context: Context, private val apiService: GitHubApiService = RetrofitClient.githubService) {

    private val owner = "mhvnsnt"
    private val repo = "M.-Engine"
    private val branch = "main"

    suspend fun saveConversationLocal(sessionId: Long, messages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        try {
            val memoryDir = File(context.filesDir, "memory")
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
            Log.e("MemoryManager", "Error saving conversation locally", e)
        }
    }
    
    suspend fun getSystemPromptLocal(): String? = withContext(Dispatchers.IO) {
        try {
            val memoryDir = File(context.filesDir, "memory")
            val file = File(memoryDir, "system-prompt.md")
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
            // For simplicity, download via raw url
            val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/memory/system-prompt.md"
            val response = apiService.downloadFile(url, authHeader)
            val content = response.string()
            
            val memoryDir = File(context.filesDir, "memory")
            if (!memoryDir.exists()) memoryDir.mkdirs()
            File(memoryDir, "system-prompt.md").writeText(content)
            Log.d("MemoryManager", "Successfully pulled system prompt from GitHub")
        } catch (e: Exception) {
            Log.e("MemoryManager", "Failed to pull system prompt", e)
        }
    }

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
            val fileName = "memory/session_$sessionId.md"

            // 1. Get current reference
            val refResponse = apiService.getReference(authHeader, owner, repo, branch)
            val baseSha = refResponse.objectInfo.sha

            // 2. Get current commit tree
            val commitResponse = apiService.getCommit(authHeader, owner, repo, baseSha)
            val baseTreeSha = commitResponse.tree.sha

            // 3. Create blob for new file
            val blobResponse = apiService.createBlob(authHeader, owner, repo, GitHubBlobRequest(content = content))

            // 4. Create new tree
            val treeRequest = GitHubTreeRequest(
                base_tree = baseTreeSha,
                tree = listOf(
                    GitHubTreeItemRequest(path = fileName, sha = blobResponse.sha)
                )
            )
            val newTree = apiService.createTree(authHeader, owner, repo, treeRequest)

            // 5. Create new commit
            val commitRequest = GitHubCreateCommitRequest(
                message = "Auto-commit: Sync session $sessionId",
                tree = newTree.sha,
                parents = listOf(baseSha)
            )
            val newCommit = apiService.createCommit(authHeader, owner, repo, commitRequest)

            // 6. Update reference
            apiService.updateReference(authHeader, owner, repo, branch, GitHubUpdateRefRequest(sha = newCommit.sha))
            Log.d("MemoryManager", "Successfully synced session $sessionId to GitHub")
        } catch (e: Exception) {
            Log.e("MemoryManager", "Failed to sync to GitHub", e)
        }
    }
}
