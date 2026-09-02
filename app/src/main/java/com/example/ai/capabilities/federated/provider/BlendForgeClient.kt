package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Enough to tell a listening worker from a dead port. */
private const val BLENDFORGE_HEALTH_TIMEOUT_MS = 3_000

private const val BLENDFORGE_CONNECT_TIMEOUT_MS = 5_000

/** A Blender conversion of a character mesh is measured in seconds to minutes. */
private const val BLENDFORGE_JOB_TIMEOUT_MS = 600_000

private const val BLENDFORGE_HTTP_OK = 200
private const val BLENDFORGE_HTTP_MULTIPLE_CHOICES = 300
private val BLENDFORGE_HTTP_SUCCESS = BLENDFORGE_HTTP_OK until BLENDFORGE_HTTP_MULTIPLE_CHOICES

/** A non-success response from the BlendForge worker. */
class BlendForgeException(val statusCode: Int, body: String) :
    IOException("BlendForge worker HTTP $statusCode: $body")

/**
 * Client for CODEDUMMY's BlendForge worker — a BullMQ/Redis queue in front of
 * headless Blender in a container.
 *
 * Blender cannot run on the phone, so this is a remote worker in exactly the
 * shape the Unreal worker uses. The client never infers capability from
 * configuration: it asks the worker whether Blender is actually present, and the
 * worker answers by having looked.
 */
class BlendForgeClient(
    private val baseUrl: String = "http://localhost:8790",
    private val token: String = "",
) {

    /** Null when healthy, otherwise WHY not — the reason is the useful part. */
    suspend fun healthError(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = BLENDFORGE_HEALTH_TIMEOUT_MS
                readTimeout = BLENDFORGE_HEALTH_TIMEOUT_MS
            }
            val code = conn.responseCode
            if (code == BLENDFORGE_HTTP_OK) null else "worker answered HTTP $code"
        } catch (e: IOException) {
            e.message ?: e.javaClass.simpleName
        }
    }

    /**
     * The worker's own report of what it can do — crucially whether a Blender
     * binary was actually found, and whether the queue is reachable.
     */
    suspend fun capabilities(): String = get("/capabilities")

    /** Enqueues one bounded operation and returns the worker's job record. */
    suspend fun submit(operation: String, payload: JSONObject): String =
        post("/op/$operation", payload.toString())

    private fun auth(conn: HttpURLConnection) {
        if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
    }

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = BLENDFORGE_CONNECT_TIMEOUT_MS
            readTimeout = BLENDFORGE_JOB_TIMEOUT_MS
            auth(this)
        }
        readOrThrow(conn)
    }

    private suspend fun post(path: String, body: String): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = BLENDFORGE_CONNECT_TIMEOUT_MS
            readTimeout = BLENDFORGE_JOB_TIMEOUT_MS
            doOutput = true
            auth(this)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }
        readOrThrow(conn)
    }

    private fun readOrThrow(conn: HttpURLConnection): String {
        if (conn.responseCode !in BLENDFORGE_HTTP_SUCCESS) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            throw BlendForgeException(conn.responseCode, err)
        }
        return conn.inputStream.bufferedReader().readText()
    }
}

/**
 * The operations this provider will dispatch, and the only ones.
 *
 * An allowlist, not a passthrough. The worker runs Blender with a Python script
 * and a shell; exposing that surface through a capability provider would make
 * the fabric an arbitrary-execution hole. Adding an operation here is a
 * deliberate act.
 */
enum class BlendForgeOperation(val wireName: String) {
    /** Read a model and report meshes, materials, skeleton, bounds. Read-only. */
    INSPECT("inspect"),

    /** Weld, centre, rescale to a stated height. Geometry-preserving. */
    NORMALIZE("normalize"),

    /** Convert between supported mesh formats (e.g. .blend/.fbx -> .glb). */
    CONVERT("convert"),
    ;

    companion object {
        fun fromWire(name: String): BlendForgeOperation? =
            entries.firstOrNull { it.wireName.equals(name, ignoreCase = true) }
    }
}
