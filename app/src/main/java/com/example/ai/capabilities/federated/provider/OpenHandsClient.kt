package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class OpenHandsClient(private val baseUrl: String = "http://localhost:3000") {
    
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2000
            conn.responseCode == 200 || conn.responseCode == 401 
        } catch (e: Exception) {
            false
        }
    }

    suspend fun dispatchSession(payload: String, token: String = ""): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/v1/sessions")
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
            throw Exception("OpenHands dispatch failed: ${e.message}", e)
        }
    }
}
