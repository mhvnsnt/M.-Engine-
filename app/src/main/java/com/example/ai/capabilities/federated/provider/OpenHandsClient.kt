package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * HTTP client for a real OpenHands instance (self-hosted or OpenHands Cloud).
 *
 * Endpoints are the documented V1 App Conversations API:
 *   POST /api/v1/app-conversations              start a conversation
 *   GET  /api/v1/app-conversations?ids=...      conversation status
 *   GET  /api/v1/app-conversations/start-tasks  poll a pending start task
 * The V0 /api/conversations API is deprecated.
 *
 * NOTE: an earlier revision of this client posted to `/api/v1/sessions`, and the
 * separate OpenHandsWorkerAdapter invented `/sandbox/provision` and
 * `/sandbox/{id}/diff`. None of those paths exist in OpenHands. Coding against
 * an imagined API is indistinguishable from a mock until something calls it.
 */
class OpenHandsClient(
    private val baseUrl: String = "http://localhost:3000",
    private val apiKey: String = "",
) {

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
            }
            // 401 still proves a server is answering; it just wants credentials.
            conn.responseCode == 200 || conn.responseCode == 401
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Starts a conversation and returns the start-task id.
     *
     * The response carries `app_conversation_id` only once `status` reaches
     * READY, so callers poll [startTaskStatus] rather than assuming the sandbox
     * is up the instant this returns.
     */
    suspend fun startConversation(instruction: String, repository: String?): String {
        val body = JSONObject().apply {
            put(
                "initial_message",
                JSONObject().put(
                    "content",
                    org.json.JSONArray().put(
                        JSONObject().put("type", "text").put("text", instruction),
                    ),
                ),
            )
            if (!repository.isNullOrBlank()) put("selected_repository", repository)
        }
        val response = post("/api/v1/app-conversations", body.toString())
        return JSONObject(response).optString("id")
    }

    /** Polls a start task. Returns its raw JSON so the caller can read status. */
    suspend fun startTaskStatus(taskId: String): String =
        get("/api/v1/app-conversations/start-tasks?ids=${enc(taskId)}")

    /** Current sandbox/execution status of a conversation, as raw JSON. */
    suspend fun conversationStatus(conversationId: String): String =
        get("/api/v1/app-conversations?ids=${enc(conversationId)}")

    /**
     * Conversation events — the transcript of what the agent actually did.
     * This is the evidence source; there is no local substitute for it.
     */
    suspend fun conversationEvents(conversationId: String): String =
        get("/api/v1/app-conversations/${enc(conversationId)}/events")

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 15000
            applyAuth(this)
        }
        readOrThrow(conn)
    }

    private suspend fun post(path: String, payload: String): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 5000
            readTimeout = 30000
            doOutput = true
            applyAuth(this)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(payload); it.flush() }
        readOrThrow(conn)
    }

    private fun applyAuth(conn: HttpURLConnection) {
        if (apiKey.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
    }

    private fun readOrThrow(conn: HttpURLConnection): String {
        if (conn.responseCode !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            throw Exception("OpenHands HTTP ${conn.responseCode}: $err")
        }
        return conn.inputStream.bufferedReader().readText()
    }
}
