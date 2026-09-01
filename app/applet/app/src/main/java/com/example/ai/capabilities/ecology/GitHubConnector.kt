package com.example.ai.capabilities.ecology

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

class GitHubConnector {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<GitHubRepoResponse>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, GitHubRepoResponse::class.java)
    )

    suspend fun discoverRepositories(username: String): List<GitHubRepoResponse> = withContext(Dispatchers.IO) {
        val url = URL("https://api.github.com/users/$username/repos?per_page=100")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("User-Agent", "M-Engine-Discovery-Sweep")
        
        if (connection.responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            return@withContext listAdapter.fromJson(json) ?: emptyList()
        } else {
            return@withContext emptyList()
        }
    }
}
