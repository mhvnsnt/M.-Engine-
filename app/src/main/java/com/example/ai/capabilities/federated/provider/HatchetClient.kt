package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class HatchetClient(private val baseUrl: String = "http://localhost:8080") {
    
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/v1/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2000
            conn.responseCode == 200 || conn.responseCode == 401 // 401 means it's there but needs auth
        } catch (e: Exception) {
            false
        }
    }

    suspend fun dispatchWorkflow(workflowName: String, payload: String, token: String = ""): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/v1/workflows/$workflowName/trigger")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            if (token.isNotEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            conn.doOutput = true
            
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }
            
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown HTTP error"
                throw Exception("HTTP ${conn.responseCode}: $err")
            }
        } catch (e: Exception) {
            throw Exception("Hatchet dispatch failed: ${e.message}", e)
        }
    }
}
