package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Enough to tell a listening worker from a dead port, and no longer. */
private const val HEALTH_TIMEOUT_MS = 3_000

/** A TCP connect either succeeds quickly or is not going to. */
private const val CONNECT_TIMEOUT_MS = 5_000

/** Probing an installed engine walks the filesystem, so it is slow. */
private const val CAPABILITY_TIMEOUT_MS = 120_000

/** Unreal builds are measured in tens of minutes, not seconds. */
private const val BUILD_TIMEOUT_MINUTES = 50L
private val BUILD_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(BUILD_TIMEOUT_MINUTES).toInt()

private const val HTTP_OK = 200
private const val HTTP_MULTIPLE_CHOICES = 300

/** The 2xx band: [200, 300). */
private val HTTP_SUCCESS = HTTP_OK until HTTP_MULTIPLE_CHOICES

/**
 * Client for the M. Engine Unreal remote worker (tools/unreal-worker).
 *
 * Unreal cannot run on the phone and cannot run in M. Engine's own container:
 * it is licence-gated and ~100 GB. The honest federation mode is therefore a
 * remote worker on a machine that actually has the engine, with the phone
 * governing and observing.
 *
 * This client never infers capability from configuration. It asks the worker,
 * and the worker answers by having looked.
 */
class UnrealWorkerClient(
    private val baseUrl: String = "http://localhost:8770",
    private val token: String = "",
) {
    /**
     * Returns null when the worker is healthy, otherwise WHY it is not.
     *
     * A boolean would discard the reason, and "connection refused" and
     * "timed out" send an operator to different places — the first says
     * nothing is listening, the second says something is and is wedged.
     */
    suspend fun healthError(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = HEALTH_TIMEOUT_MS
                readTimeout = HEALTH_TIMEOUT_MS
            }
            val code = conn.responseCode
            if (code == HTTP_OK) null else "worker answered HTTP $code"
        } catch (e: java.io.IOException) {
            e.message ?: e.javaClass.simpleName
        }
    }

    /** Raw capability report. Probing an engine can be slow, hence the long read timeout. */
    suspend fun capabilities(): String = get("/capabilities", readTimeoutMs = CAPABILITY_TIMEOUT_MS)

    suspend fun inspectContent(uproject: String): String =
        post("/op/inspectContent", JSONObject().put("uproject", uproject).toString(), CAPABILITY_TIMEOUT_MS)

    /** A real compile. The first point at which a claim about the C++ becomes verifiable. */
    suspend fun build(uproject: String, target: String? = null): String =
        post(
            "/op/build",
            JSONObject().put("uproject", uproject).apply { target?.let { put("target", it) } }.toString(),
            BUILD_TIMEOUT_MS,
        )

    private fun auth(conn: HttpURLConnection) {
        if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
    }

    private suspend fun get(path: String, readTimeoutMs: Int): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            auth(this)
        }
        readOrThrow(conn)
    }

    private suspend fun post(path: String, body: String, readTimeoutMs: Int): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = readTimeoutMs
                doOutput = true
                auth(this)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }
            readOrThrow(conn)
        }

    private fun readOrThrow(conn: HttpURLConnection): String {
        if (conn.responseCode !in HTTP_SUCCESS) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            throw UnrealWorkerException(conn.responseCode, err)
        }
        return conn.inputStream.bufferedReader().readText()
    }
}

/** A non-success response from the worker, carrying the code and its body. */
class UnrealWorkerException(val statusCode: Int, body: String) :
    java.io.IOException("Unreal worker HTTP $statusCode: $body")

/**
 * Registers Unreal as a first-class capability in the fabric.
 *
 * The provider is AVAILABLE only when a worker answers AND that worker reports
 * an actually-discovered engine. A reachable worker on a machine without Unreal
 * is `PARTIALLY_VERIFIED`, not available — the distinction matters, because
 * "the worker is up" and "Unreal can build" are different facts and conflating
 * them is how a green light stops meaning anything.
 */
class UnrealExecutionProvider(
    private val client: UnrealWorkerClient = UnrealWorkerClient(),
) : CapabilityProvider {

    override val providerId: String = "unreal_remote_worker"
    override val capabilityType: CapabilityType = CapabilityType.GAME_ENGINE_BUILD

    override suspend fun probe(): CapabilityProbeResult {
        val healthError = client.healthError()
        if (healthError != null) {
            return CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: no Unreal worker reachable ($healthError). Run " +
                    "tools/unreal-worker on a machine with Unreal Engine installed.",
            )
        }

        return runCatching { JSONObject(client.capabilities()) }.fold(
            onSuccess = { report -> classify(report) },
            onFailure = { e ->
                CapabilityProbeResult(
                    status = FabricNodeState.PARTIALLY_VERIFIED,
                    error = "worker reachable but capability report unreadable: ${e.message}",
                )
            },
        )
    }

    /**
     * A worker that is up but has no engine is a real, useful, and DIFFERENT
     * state from one that can build.
     */
    private fun classify(report: JSONObject): CapabilityProbeResult {
        val caps = report.optJSONObject("capabilities") ?: JSONObject()
        val engine = caps.optJSONObject("UNREAL_RUNTIME_DISCOVERED") ?: JSONObject()
        val details = detailsFrom(report, caps, engine)
        return when (engine.optString("state")) {
            "VERIFIED" -> CapabilityProbeResult(FabricNodeState.AVAILABLE, details)
            "PARTIALLY_VERIFIED" -> CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "engine binary present but version probe failed: " +
                    engine.optString("evidence"),
            )
            else -> CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "CAPABILITY_GAP: worker reachable but no Unreal Engine on that host — " +
                    engine.optString("evidence"),
            )
        }
    }

    /** Flattens the worker's report into the detail map the fabric displays. */
    private fun detailsFrom(
        report: JSONObject,
        caps: JSONObject,
        engine: JSONObject,
    ): Map<String, String> {
        val host = report.optJSONObject("host") ?: JSONObject()
        return buildMap {
            put("host", host.optString("hostname", "unknown"))
            put("platform", host.optString("platform", "unknown"))
            // optString yields the literal "null" for a JSON null, which would
            // otherwise be displayed as a version.
            engine.optString("version").takeIf { it.isNotBlank() && it != "null" }
                ?.let { put("engineVersion", it) }
            engine.optString("engineRoot").takeIf { it.isNotBlank() && it != "null" }
                ?.let { put("engineRoot", it) }
            listOf(
                "UNREAL_BUILD_CAPABLE", "UNREAL_PROJECT_AVAILABLE",
                "ANDROID_TOOLCHAIN_AVAILABLE", "PHYSICAL_DEVICE_AVAILABLE",
            ).forEach { key ->
                caps.optJSONObject(key)?.optString("state")?.let { put(key, it) }
            }
        }
    }

    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask,
    ): CapabilityExecutionResult {
        val probe = probe()
        // contextPayload carries the .uproject path; the worker independently
        // refuses anything outside its configured project roots.
        val blocker = when {
            probe.status != FabricNodeState.AVAILABLE ->
                "BLOCKED: Unreal is not available on the connected worker."
            task.contextPayload.isBlank() -> "no .uproject supplied in contextPayload"
            else -> null
        }
        if (blocker != null) {
            return CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "",
                stderr = probe.error.orEmpty(), error = blocker,
            )
        }
        val uproject = task.contextPayload

        return try {
            val raw = client.build(uproject)
            val json = JSONObject(raw)
            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = json.optInt("exitCode", -1),
                stdout = json.optString("stdout"),
                stderr = json.optString("stderr"),
                // The build log is the evidence, whether it passed or failed.
                returnedEvidencePayload = raw,
            )
        } catch (e: java.io.IOException) {
            CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "",
                stderr = e.message ?: "unknown error", error = "EXECUTION_FAILED",
            )
        } catch (e: JSONException) {
            CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "",
                stderr = e.message ?: "unreadable worker response",
                error = "EXECUTION_FAILED",
            )
        }
    }
}
