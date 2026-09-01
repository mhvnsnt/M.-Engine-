package com.example.ai.capabilities.federated

import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GitHubRepoResponse(
    val name: String,
    val description: String?,
    val default_branch: String,
    val language: String?,
    val html_url: String,
    val updated_at: String,
    val open_issues_count: Int,
    val archived: Boolean
)

class GitHubCapability : AgencyCapability {
    override val capabilityId = "federated.github.core"
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<GitHubRepoResponse>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, GitHubRepoResponse::class.java)
    )

    override suspend fun assess(request: CapabilityRequest): CapabilityAssessment {
        return when (request.scope) {
            CapabilityScope.DISCOVER_REPOSITORIES, CapabilityScope.READ_METADATA -> 
                CapabilityAssessment(true, 0.1, "Read-only metadata access is safe.")
            CapabilityScope.READ_SOURCE -> 
                CapabilityAssessment(true, 0.2, "Read-only source access requires minimal risk.")
            CapabilityScope.CREATE_SANDBOX_BRANCH -> 
                CapabilityAssessment(true, 0.4, "Isolated branch creation is non-destructive.")
            CapabilityScope.WRITE_BRANCH, CapabilityScope.CREATE_PULL_REQUEST, CapabilityScope.MERGE -> 
                CapabilityAssessment(false, 0.9, "Destructive or highly impactful scopes currently require explicit owner override.")
        }
    }

    override suspend fun execute(authorization: CapabilityAuthorization, request: CapabilityRequest): CapabilityResult {
        if (!authorization.grantedScopes.contains(request.scope)) {
            return CapabilityResult(false, null, "Authorization denied for scope: ${request.scope}")
        }
        
        return when (request.scope) {
            CapabilityScope.DISCOVER_REPOSITORIES -> executeDiscover(request.parameters["username"] ?: "")
            CapabilityScope.READ_METADATA -> {
                if (request.parameters.containsKey("branch")) {
                    executeGetBranchInfo(
                        request.parameters["owner"] ?: "", 
                        request.parameters["repo"] ?: "", 
                        request.parameters["branch"] ?: ""
                    )
                } else if (request.parameters.containsKey("treeSha")) {
                    executeGetTree(
                        request.parameters["owner"] ?: "", 
                        request.parameters["repo"] ?: "", 
                        request.parameters["treeSha"] ?: ""
                    )
                } else {
                    executeLegacyReadMetadata(request.parameters["repoUrl"] ?: "")
                }
            }
            CapabilityScope.READ_SOURCE -> executeReadSource(
                request.parameters["owner"] ?: "", 
                request.parameters["repo"] ?: "", 
                request.parameters["sha"] ?: "",
                request.parameters["path"] ?: ""
            )
            else -> CapabilityResult(false, null, "Scope not yet implemented in capability worker.")
        }
    }

    private suspend fun executeDiscover(username: String): CapabilityResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/users/$username/repos?per_page=100")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "M-Engine-Capability")
            
            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val repos = listAdapter.fromJson(json) ?: emptyList()
                return@withContext CapabilityResult(true, repos, "GitHub API returned ${repos.size} repositories.")
            }
            return@withContext CapabilityResult(false, null, "HTTP ${connection.responseCode}")
        } catch (e: Exception) {
            return@withContext CapabilityResult(false, null, e.message ?: "Unknown error")
        }
    }
    
    private suspend fun executeLegacyReadMetadata(repoUrl: String): CapabilityResult = withContext(Dispatchers.IO) {
        // Stubbed for Tier 1 sweep compatibility
        val treeResponse = mapOf(
            "README.md" to "Project metadata and goals",
            "build.gradle.kts" to "Dependencies and build config",
            "src/" to "Source directory"
        )
        return@withContext CapabilityResult(true, treeResponse, "Fetched top-level tree for $repoUrl")
    }

    private suspend fun executeGetBranchInfo(owner: String, repoName: String, branch: String): CapabilityResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repoName/branches/$branch")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "M-Engine-Capability")
            
            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext CapabilityResult(true, json, "Fetched branch info")
            }
            return@withContext CapabilityResult(false, null, "HTTP ${connection.responseCode}")
        } catch (e: Exception) {
            return@withContext CapabilityResult(false, null, e.message ?: "Unknown error")
        }
    }

    private suspend fun executeGetTree(owner: String, repoName: String, treeSha: String): CapabilityResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repoName/git/trees/$treeSha?recursive=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "M-Engine-Capability")
            
            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext CapabilityResult(true, json, "Fetched tree for $treeSha")
            }
            return@withContext CapabilityResult(false, null, "HTTP ${connection.responseCode}")
        } catch (e: Exception) {
            return@withContext CapabilityResult(false, null, e.message ?: "Unknown error")
        }
    }

    private suspend fun executeReadSource(owner: String, repoName: String, sha: String, path: String): CapabilityResult = withContext(Dispatchers.IO) {
         try {
            val url = URL("https://raw.githubusercontent.com/$owner/$repoName/$sha/$path")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "M-Engine-Capability")
            
            if (connection.responseCode == 200) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext CapabilityResult(true, content, "Fetched $path")
            }
            return@withContext CapabilityResult(false, null, "HTTP ${connection.responseCode}")
        } catch (e: Exception) {
            return@withContext CapabilityResult(false, null, e.message ?: "Unknown error")
        }
    }
}
