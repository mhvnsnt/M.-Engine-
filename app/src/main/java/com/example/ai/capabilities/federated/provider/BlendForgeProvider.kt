package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val EXIT_FAILURE = -1

/**
 * Federates CODEDUMMY's BlendForge worker as an asset-pipeline capability.
 *
 * WHY FEDERATE RATHER THAN REBUILD. Bannon already normalises models — welding,
 * satellite stripping, weight transfer, rescale, decimation — but as local
 * scripts a human runs one at a time. BlendForge is the same class of work
 * already packaged as a queued, containerised worker. The gap was never the
 * algorithms; it was that nothing could ASK for a conversion and get a recorded
 * artifact back.
 *
 * THREE EVIDENCE STAGES, DELIBERATELY NOT COLLAPSED:
 *
 *   worker reachable        !=  Blender present
 *   Blender present         !=  job completed
 *   job completed           !=  the artifact is what was asked for
 *
 * `probe()` can only ever establish the first two. A green probe means a worker
 * answered and told us it found Blender — nothing about any job. That
 * distinction is the whole reason this provider reports PARTIALLY_VERIFIED
 * rather than AVAILABLE when the worker is up but has no Blender.
 */
class BlendForgeProvider(
    private val client: BlendForgeClient = BlendForgeClient(),
) : CapabilityProvider {

    override val providerId: String = "blendforge_asset_worker"
    override val capabilityType: CapabilityType = CapabilityType.ASSET_PIPELINE

    override suspend fun probe(): CapabilityProbeResult {
        val healthError = client.healthError()
        if (healthError != null) {
            return CapabilityProbeResult(
                status = FabricNodeState.UNAVAILABLE,
                error = "CAPABILITY_GAP: no BlendForge worker reachable ($healthError). " +
                    "Run CODEDUMMY's blendforge/ container on a host with Blender.",
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
     * A worker that is up but has no Blender, or no queue, is a real and
     * DIFFERENT state from one that can convert. Collapsing them is how a green
     * light stops meaning anything.
     */
    private fun classify(report: JSONObject): CapabilityProbeResult {
        val caps = report.optJSONObject("capabilities") ?: JSONObject()
        val blender = caps.optJSONObject("BLENDER_RUNTIME_DISCOVERED") ?: JSONObject()
        val queue = caps.optJSONObject("QUEUE_REACHABLE") ?: JSONObject()
        val details = detailsFrom(report, blender, queue)

        if (queue.optString("state") != "VERIFIED") {
            return CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "worker reachable but its Redis/BullMQ queue is not — " +
                    "jobs would be accepted and never run: ${queue.optString("evidence")}",
            )
        }
        return when (blender.optString("state")) {
            "VERIFIED" -> CapabilityProbeResult(FabricNodeState.AVAILABLE, details)
            else -> CapabilityProbeResult(
                FabricNodeState.PARTIALLY_VERIFIED, details,
                error = "CAPABILITY_GAP: worker reachable but no Blender on that host — " +
                    blender.optString("evidence"),
            )
        }
    }

    private fun detailsFrom(
        report: JSONObject,
        blender: JSONObject,
        queue: JSONObject,
    ): Map<String, String> {
        val host = report.optJSONObject("host") ?: JSONObject()
        return buildMap {
            put("host", host.optString("hostname", "unknown"))
            put("platform", host.optString("platform", "unknown"))
            // optString yields the literal "null" for a JSON null.
            blender.optString("version").takeIf { it.isNotBlank() && it != "null" }
                ?.let { put("blenderVersion", it) }
            put("queue", queue.optString("state", "UNKNOWN"))
            put("operations", BlendForgeOperation.entries.joinToString(",") { it.wireName })
        }
    }

    /**
     * Dispatches ONE allowlisted operation.
     *
     * `task.objective` names the operation and `task.contextPayload` carries its
     * JSON arguments. An objective outside [BlendForgeOperation] is refused here
     * rather than forwarded — the worker's own surface is a Blender process with
     * a shell behind it, and a capability provider must not be a hole into it.
     */
    override suspend fun execute(
        authorization: CapabilityAuthorization,
        task: CapabilityTask,
    ): CapabilityExecutionResult {
        val probe = probe()
        val operation = BlendForgeOperation.fromWire(task.objective.trim())
        val blocker = when {
            probe.status != FabricNodeState.AVAILABLE ->
                "BLOCKED: BlendForge is not available on the connected worker."
            operation == null ->
                "REFUSED: '${task.objective}' is not an allowlisted BlendForge operation. " +
                    "Allowed: " + BlendForgeOperation.entries.joinToString(", ") { it.wireName }
            else -> null
        }
        if (blocker != null) {
            return CapabilityExecutionResult(
                taskId = task.taskId, exitCode = EXIT_FAILURE, stdout = "",
                stderr = probe.error.orEmpty(), error = blocker,
            )
        }

        val payload = runCatching { JSONObject(task.contextPayload) }.getOrElse { JSONObject() }
        return try {
            val raw = client.submit(requireNotNull(operation).wireName, payload)
            val json = JSONObject(raw)
            CapabilityExecutionResult(
                taskId = task.taskId,
                exitCode = json.optInt("exitCode", EXIT_FAILURE),
                stdout = json.optString("stdout"),
                stderr = json.optString("stderr"),
                // The worker's record IS the evidence — it carries the output
                // path and the worker's own sha256 of the bytes it wrote. The
                // Artifact Authority verifies that hash against the file; this
                // provider does not get to assert it.
                returnedEvidencePayload = raw,
            )
        } catch (e: IOException) {
            failure(task, e.message ?: "transport failure")
        } catch (e: JSONException) {
            failure(task, e.message ?: "unreadable worker response")
        }
    }

    private fun failure(task: CapabilityTask, message: String) = CapabilityExecutionResult(
        taskId = task.taskId, exitCode = EXIT_FAILURE, stdout = "",
        stderr = message, error = "EXECUTION_FAILED",
    )
}
