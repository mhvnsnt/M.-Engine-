package com.example.ai.capabilities

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JGitRepositoryManager(
    private val localCacheDir: File,
    private val authToken: String
) {
    suspend fun cloneRepository(repoUrl: String, repoName: String): File = withContext(Dispatchers.IO) {
        val targetDir = File(localCacheDir, repoName)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        
        val credentials = UsernamePasswordCredentialsProvider("token", authToken)
        
        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(targetDir)
            .setCredentialsProvider(credentials)
            .call().use { git ->
                // Cloned successfully
            }
        targetDir
    }
    
    suspend fun createBranch(repoDir: File, branchName: String) = withContext(Dispatchers.IO) {
        Git.open(repoDir).use { git ->
            git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .call()
        }
    }
    
    suspend fun commitAndPush(repoDir: File, commitMessage: String) = withContext(Dispatchers.IO) {
        val credentials = UsernamePasswordCredentialsProvider("token", authToken)
        
        Git.open(repoDir).use { git ->
            // Add all changes
            git.add().addFilepattern(".").call()
            
            // Commit
            git.commit().setMessage(commitMessage).call()
            
            // Push
            git.push()
                .setCredentialsProvider(credentials)
                .call()
        }
    }
}
