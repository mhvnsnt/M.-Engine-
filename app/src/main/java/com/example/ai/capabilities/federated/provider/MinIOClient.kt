package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MinIOClient(private val baseUrl: String = "http://localhost:9000") {
    
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/minio/health/live")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2000
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}
