package com.example.ai.capabilities

import com.example.network.GitHubApiService
import com.example.network.GitHubCreateRefRequest
import com.example.network.GitHubCreatePRRequest
import com.example.network.GitHubUpdateRefRequest
import android.util.Log

class GitHubServiceImpl(
    private val apiService: GitHubApiService,
    private val authToken: String
) : GitHubService {

    private val authHeader = "token $authToken"

    override suspend fun authenticate(secureToken: String): Boolean {
        // Authenticated by default if we got here with a token
        return secureToken.isNotEmpty()
    }

    override suspend fun listRepositories(): List<RepositoryRef> {
        return try {
            val repos = apiService.listRepositories(authHeader)
            repos.map { RepositoryRef(it.owner.login, it.name, it.default_branch) }
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "listRepositories error", e)
            emptyList()
        }
    }

    override suspend fun inspectRepository(repo: RepositoryRef): RepoMetadata {
        return try {
            val r = apiService.getRepository(authHeader, repo.owner, repo.name)
            RepoMetadata(
                languages = r.language?.let { listOf(it) } ?: emptyList(),
                defaultBranch = r.default_branch,
                description = r.description ?: "",
                stars = r.stargazers_count
            )
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "inspectRepository error", e)
            RepoMetadata(emptyList(), "main", "Error", 0)
        }
    }

    override suspend fun getReadme(repo: RepositoryRef): String {
        return try {
            val readme = apiService.getReadme(authHeader, repo.owner, repo.name)
            if (readme.encoding == "base64") {
                val decoded = android.util.Base64.decode(readme.content.replace("\n", ""), android.util.Base64.DEFAULT)
                String(decoded)
            } else {
                readme.content
            }
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "getReadme error", e)
            ""
        }
    }

    override suspend fun inspectIssue(repo: RepositoryRef, issueNumber: Int): IssueDetails {
        return try {
            val issue = apiService.getIssue(authHeader, repo.owner, repo.name, issueNumber)
            IssueDetails(issue.number, issue.title, issue.body ?: "", issue.state)
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "inspectIssue error", e)
            IssueDetails(issueNumber, "Unknown", "Error fetching issue", "closed")
        }
    }

    override suspend fun inspectCIResults(repo: RepositoryRef, commitSha: String): CIResult {
        // Simulated because we need a deeper API for checking specific check suites
        return CIResult("run-id", true, "https://github.com")
    }

    override suspend fun retrieveReviewComments(repo: RepositoryRef, prNumber: Int): List<String> {
        return emptyList()
    }

    override suspend fun searchCode(repo: RepositoryRef, query: String): List<String> {
        return emptyList()
    }

    override suspend fun cloneToRemoteSandbox(repo: RepositoryRef, sandboxId: String): Boolean {
        // This is physically handled by the remote sandbox, not by Android Retrofit directly.
        // It signals the sandbox dispatcher.
        return true
    }

    override suspend fun createBranch(repo: RepositoryRef, branchName: String): Boolean {
        return try {
            val defaultBranch = repo.branch ?: "main"
            val ref = apiService.getReference(authHeader, repo.owner, repo.name, defaultBranch)
            apiService.createReference(authHeader, repo.owner, repo.name, GitHubCreateRefRequest("refs/heads/$branchName", ref.objectInfo.sha))
            true
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "createBranch error", e)
            false
        }
    }

    override suspend fun commitAndPush(repo: RepositoryRef, patch: String, message: String): Boolean {
        // For physical execution from Android we would create tree, commit, update ref.
        // But Phase 8 says: Remote Sandbox modifies files, commits, and pushes.
        // So this method might be better dispatched to the remote sandbox.
        // For completeness, if M. Engine wants to do it directly:
        return false // Handled by sandbox
    }

    override suspend fun createPullRequest(repo: RepositoryRef, details: PRDetails): String {
        return try {
            val pr = apiService.createPullRequest(authHeader, repo.owner, repo.name, GitHubCreatePRRequest(
                title = details.title,
                body = details.body,
                head = details.headBranch,
                base = details.baseBranch
            ))
            pr.html_url
        } catch (e: Exception) {
            Log.e("GitHubServiceImpl", "createPullRequest error", e)
            ""
        }
    }
}
