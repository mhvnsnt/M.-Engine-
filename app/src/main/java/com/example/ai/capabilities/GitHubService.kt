package com.example.ai.capabilities

data class RepositoryRef(
    val owner: String,
    val name: String,
    val branch: String? = null
)

data class RepoMetadata(
    val languages: List<String>,
    val defaultBranch: String,
    val description: String,
    val stars: Int
)

data class PRDetails(
    val title: String,
    val body: String,
    val headBranch: String,
    val baseBranch: String
)

data class CIResult(
    val runId: String,
    val passed: Boolean,
    val logsUrl: String
)

data class IssueDetails(
    val number: Int,
    val title: String,
    val body: String,
    val state: String
)

interface GitHubService {
    suspend fun authenticate(secureToken: String): Boolean
    suspend fun listRepositories(): List<RepositoryRef>
    suspend fun inspectRepository(repo: RepositoryRef): RepoMetadata
    suspend fun getReadme(repo: RepositoryRef): String
    suspend fun inspectIssue(repo: RepositoryRef, issueNumber: Int): IssueDetails
    suspend fun inspectCIResults(repo: RepositoryRef, commitSha: String): CIResult
    suspend fun retrieveReviewComments(repo: RepositoryRef, prNumber: Int): List<String>
    suspend fun searchCode(repo: RepositoryRef, query: String): List<String>
    
    // Sandbox preparation
    suspend fun cloneToRemoteSandbox(repo: RepositoryRef, sandboxId: String): Boolean
    
    // Mutating operations - require strict capability gating / approval
    suspend fun createBranch(repo: RepositoryRef, branchName: String): Boolean
    suspend fun commitAndPush(repo: RepositoryRef, patch: String, message: String): Boolean
    suspend fun createPullRequest(repo: RepositoryRef, details: PRDetails): String
}
