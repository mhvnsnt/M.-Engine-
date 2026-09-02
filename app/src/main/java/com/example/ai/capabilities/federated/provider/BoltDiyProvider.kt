package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val BOLT_HEALTH_TIMEOUT_MS = 3_000
private const val BOLT_CONNECT_TIMEOUT_MS = 5_000
private const val BOLT_READ_TIMEOUT_MS = 30_000
private const val BOLT_HTTP_OK = 200
private const val BOLT_EXIT_FAILURE = -1

/**
 * Client for a running bolt.diy instance.
 *
 * bolt.diy is a Remix web application. Its own routes are the interface; there
 * is no separate API to invent. `/api/configured-providers` is used as the
 * readiness probe because it answers only once the server has actually loaded
 * its provider registry — a plain page fetch would return HTML from a server
 * that cannot yet route a model call.
 */
class BoltDiyClient(private val baseUrl: String = "http://localhost:5173") {

    /** Null when healthy, otherwise the reason. */
    suspend fun healthError(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = open("/api/configured-providers", BOLT_HEALTH_TIMEOUT_MS)
            val code = conn.responseCode
            if (code == BOLT_HTTP_OK) null else "bolt.diy answered HTTP $code"
        } catch (e: IOException) {
            e.message ?: e.javaClass.simpleName
        }
    }

    /** The provider registry the running instance actually has configured. */
    suspend fun configuredProviders(): String = withContext(Dispatchers.IO) {
        val conn = open("/api/configured-providers", BOLT_READ_TIMEOUT_MS)
        if (conn.responseCode != BOLT_HTTP_OK) {
            throw IOException("bolt.diy HTTP ${conn.responseCode} reading providers")
        }
        conn.inputStream.bufferedReader().readText()
    }

    private fun open(path: String, readTimeoutMs: Int): HttpURLConnection =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = BOLT_CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
        }
}

/**
 * Federates a running bolt.diy instance as M. Engine's development workbench.
 *
 * WHY THIS EXISTS. `M_ENGINE_REALITY_VERIFICATION_REPORT.md` records that the
 * code IDE, workbench and sandbox sections have "no implementation to verify".
 * The Owner already runs a fork of a mature system that implements them:
 * WebContainer sandboxing, an action runner, a message-parser write protocol,
 * git operations, and 22 model providers including ollama and lmstudio.
 *
 * THE BOUNDARY IS THE POINT. This provider is an adapter, not a port. None of
 * bolt.diy's implementation is copied here; M. Engine keeps authority over what
 * was permitted and what counts as evidence, and bolt.diy owns execution. The
 * alternative — reimplementing an IDE inside an Android app — is the exact
 * duplicate-subsystem trap the capability graph exists to prevent.
 *
 * A REPOSITORY IS NOT A CAPABILITY. This reports AVAILABLE only when an
 * instance actually answers. Having the source checked out proves nothing, and
 * the fabric must never imply otherwise.
 */
class BoltDiyProvider(
    private val client: BoltDiyClient = BoltDiyClient(),
) : CapabilityProvider {

    override val providerId: String = "boltdiy_workbench"
    override val capabilityType: CapabilityType = CapabilityType.DEVELOPMENT_WORKBENCH

    override suspend fun probe(): CapabilityProbeResult {
        val healthError = client.healthError()
        if (healthError != null) {
            return CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: no bolt.diy instance reachable ($healthError). " +
                    "The repository being present is not the capability — an instance must run.",
            )
        }
        return runCatching { providerNames(client.configuredProviders()) }.fold(
            onSuccess = { names -> classify(names) },
            onFailure = { e ->
                CapabilityProbeResult(
                    status = FabricNodeState.PARTIALLY_VERIFIED,
                    error = "bolt.diy reachable but its provider registry was unreadable: ${e.message}",
                )
            },
        )
    }

    /**
     * Reachable with zero configured model providers is a real and different
     * state from reachable and able to route a call. The editor and git surface
     * still work; model inference does not.
     */
    private fun classify(names: List<String>): CapabilityProbeResult {
        val details = mapOf(
            "modelProviders" to names.size.toString(),
            "providers" to names.sorted().joinToString(","),
            // Named so the catalogue shows what federating this actually bought,
            // rather than a bare AVAILABLE.
            "federates" to "editor,workspace,repository_ops,sandbox_execution,model_routing",
        )
        if (names.isEmpty()) {
            return CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "bolt.diy is up but has NO configured model providers — " +
                    "workspace and repository operations would work, model routing would not.",
            )
        }
        return CapabilityProbeResult(FabricNodeState.AVAILABLE, details)
    }

    private fun providerNames(raw: String): List<String> {
        val json = JSONArray(raw)
        return (0 until json.length()).mapNotNull { i ->
            when (val entry = json.opt(i)) {
                is String -> entry
                else -> json.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
            }
        }
    }

    /**
     * Not implemented, and reported as such.
     *
     * Dispatching real work needs a bounded operation set agreed against a
     * RUNNING instance — bolt.diy's routes are a private interface that moves
     * between versions, and guessing at them here would produce a provider that
     * looks wired and fails on first use. Probing is honest today; execution
     * waits for an instance to design against.
     */
    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask,
    ): CapabilityExecutionResult = CapabilityExecutionResult(
        taskId = task.taskId,
        exitCode = BOLT_EXIT_FAILURE,
        stdout = "",
        stderr = "",
        error = "NOT_IMPLEMENTED: bolt.diy execution operations are not defined yet. " +
            "Probe reports real availability; dispatch needs a bounded operation set " +
            "agreed against a running instance.",
    )
}
