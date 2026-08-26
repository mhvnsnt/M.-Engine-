with open("app/src/main/java/com/example/network/GitHubApiService.kt", "r") as f:
    content = f.read()

new_endpoints = """
    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getReference(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): GitHubRefResponse

    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    suspend fun getCommit(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GitHubCommitResponse

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @retrofit2.http.Body request: GitHubBlobRequest
    ): GitHubBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @retrofit2.http.Body request: GitHubTreeRequest
    ): GitHubTreeResponseInfo

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @retrofit2.http.Body request: GitHubCreateCommitRequest
    ): GitHubCommitResponse

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateReference(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @retrofit2.http.Body request: GitHubUpdateRefRequest
    ): GitHubRefResponse
"""

if "getReference" not in content:
    content = content.replace("}", new_endpoints + "\n}")
    
    models = """
data class GitHubRefResponse(val ref: String, val objectInfo: GitHubRefObject) {
    @com.squareup.moshi.Json(name = "object") val obj: GitHubRefObject get() = objectInfo
}
data class GitHubRefObject(val sha: String, val type: String)
data class GitHubCommitResponse(val sha: String, val tree: GitHubCommitTree)
data class GitHubCommitTree(val sha: String)
data class GitHubBlobRequest(val content: String, val encoding: String = "utf-8")
data class GitHubBlobResponse(val sha: String, val url: String)
data class GitHubTreeRequest(val base_tree: String, val tree: List<GitHubTreeItemRequest>)
data class GitHubTreeItemRequest(val path: String, val mode: String = "100644", val type: String = "blob", val sha: String)
data class GitHubTreeResponseInfo(val sha: String, val url: String)
data class GitHubCreateCommitRequest(val message: String, val tree: String, val parents: List<String>)
data class GitHubUpdateRefRequest(val sha: String, val force: Boolean = true)
"""
    content = content + "\n" + models

with open("app/src/main/java/com/example/network/GitHubApiService.kt", "w") as f:
    f.write(content)

