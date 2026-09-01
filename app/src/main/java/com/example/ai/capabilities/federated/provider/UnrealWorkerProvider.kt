package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 3000; readTimeout = 3000
            }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /** Raw capability report. Probing an engine can be slow, hence the long read timeout. */
    suspend fun capabilities(): String = get("/capabilities", readTimeoutMs = 120_000)

    suspend fun inspectContent(uproject: String): String =
        post("/op/inspectContent", JSONObject().put("uproject", uproject).toString(), 120_000)

    /** A real compile. The first point at which a claim about the C++ becomes verifiable. */
    suspend fun build(uproject: String, target: String? = null): String =
        post(
            "/op/build",
            JSONObject().put("uproject", uproject).apply { target?.let { put("target", it) } }.toString(),
            // Unreal builds are measured in tens of minutes, not seconds.
            50 * 60 * 1000,
        )

    private fun auth(conn: HttpURLConnection) {
        if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
    }

    private suspend fun get(path: String, readTimeoutMs: Int): String = withContext(Dispatchers.IO) {
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 5000; readTimeout = readTimeoutMs; auth(this)
        }
        readOrThrow(conn)
    }

    private suspend fun post(path: String, body: String, readTimeoutMs: Int): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000; readTimeout = readTimeoutMs; doOutput = true; auth(this)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }
            readOrThrow(conn)
        }

    private fun readOrThrow(conn: HttpURLConnection): String {
        if (conn.responseCode !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            throw Exception("Unreal worker HTTP ${conn.responseCode}: $err")
        }
        return conn.inputStream.bufferedReader().readText()
    }
}

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
        if (!client.checkHealth()) {
            return CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: no Unreal worker reachable. Run tools/unreal-worker " +
                    "on a machine with Unreal Engine installed.",
            )
        }

        val report = try {
            JSONObject(client.capabilities())
        } catch (e: Exception) {
            return CapabilityProbeResult(
                status = FabricNodeState.PARTIALLY_VERIFIED,
                error = "worker reachable but capability report unreadable: ${e.message}",
            )
        }

        val caps = report.optJSONObject("capabilities") ?: JSONObject()
        val engine = caps.optJSONObject("UNREAL_RUNTIME_DISCOVERED") ?: JSONObject()
        val engineState = engine.optString("state")
        val host = report.optJSONObject("host") ?: JSONObject()

        val details = buildMap {
            put("host", host.optString("hostname", "unknown"))
            put("platform", host.optString("platform", "unknown"))
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

        // A worker that is up but has no engine is a real, useful, and DIFFERENT
        // state from one that can build.
        return when (engineState) {
            "VERIFIED" -> CapabilityProbeResult(FabricNodeState.AVAILABLE, details)
            "PARTIALLY_VERIFIED" -> CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "engine binary present but version probe failed: ${engine.optString("evidence")}",
            )
            else -> CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "CAPABILITY_GAP: worker reachable but no Unreal Engine on that host — " +
                    engine.optString("evidence"),
            )
        }
    }

    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask,
    ): CapabilityExecutionResult {
        val probe = probe()
        if (probe.status != FabricNodeState.AVAILABLE) {
            return CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "", stderr = probe.error.orEmpty(),
                error = "BLOCKED: Unreal is not available on the connected worker.",
            )
        }
        // contextPayload carries the .uproject path; the worker independently
        // refuses anything outside its configured project roots.
        val uproject = task.contextPayload.takeIf { it.isNotBlank() }
            ?: return CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "", stderr = "",
                error = "no .uproject supplied in contextPayload",
            )

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
        } catch (e: Exception) {
            CapabilityExecutionResult(
                taskId = task.taskId, exitCode = -1, stdout = "",
                stderr = e.message ?: "unknown error", error = "EXECUTION_FAILED",
            )
        }
    }
}
