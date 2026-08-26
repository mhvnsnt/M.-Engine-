import re

with open("app/src/main/java/com/example/network/GitHubApiService.kt", "r") as f:
    content = f.read()

target = """interface GitHubApiService {"""
new = """
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunsResponse(
    val total_count: Int,
    val workflow_runs: List<GitHubWorkflowRun>
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val updated_at: String
)

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @retrofit2.http.Query("per_page") perPage: Int = 5
    ): GitHubWorkflowRunsResponse
"""
content = content.replace(target, new)

with open("app/src/main/java/com/example/network/GitHubApiService.kt", "w") as f:
    f.write(content)
