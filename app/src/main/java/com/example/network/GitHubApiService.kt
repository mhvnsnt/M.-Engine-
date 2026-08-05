package com.example.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Url
import okhttp3.ResponseBody

interface GitHubApiService {
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
