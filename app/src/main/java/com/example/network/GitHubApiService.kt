package com.example.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Url
import retrofit2.http.POST
import retrofit2.http.PATCH

import okhttp3.ResponseBody


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

    @GET("repos/{owner}/{repo}/git/trees/{branch}?recursive=1")
    suspend fun getRepoTree(
        @Header("Authorization") auth: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): GitHubTreeResponse

    @GET
    suspend fun downloadFile(
        @Url url: String,
        @Header("Authorization") auth: String?,
        @Header("Accept") accept: String = "application/vnd.github.v3.raw"
    ): ResponseBody

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
}

data class GitHubTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeItem>,
    val truncated: Boolean
)

data class GitHubTreeItem(
    val path: String,
    val mode: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Long?,
    val url: String
)


data class GitHubRefResponse(val ref: String, @com.squareup.moshi.Json(name = "object") val objectInfo: GitHubRefObject)
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
