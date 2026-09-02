package com.example.ai.cloud

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun startKtorServer(port: Int, ledger: AgencyLedgerRepository) {
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            gson { }
        }
        installCors()
        routing {
            get("/health") {
                call.respond(mapOf("status" to "UP"))
            }
            get("/ready") {
                try {
                    ledger.isAutonomyEnabled()
                    call.respond(mapOf("status" to "READY"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "DOWN", "error" to e.message))
                }
            }
            
        post("/api/v1/ledger/sync") {
            try {
                val payload = call.receive<List<Map<String, Any>>>()
                val result = ledger.syncConversationEvents(payload)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid payload")))
            }
        }
        
        get("/api/v1/ledger/events") {
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val events = ledger.getConversationEvents(since)
            call.respond(events)
        }

        get("/api/v1/mindstream") {
                val stream = ledger.getMindstream()
                call.respond(stream)
            }
            get("/api/v1/opportunities") {
                val opps = ledger.getPendingOpportunities()
                call.respond(opps)
            }
            get("/api/v1/control_plane") {
                val isEnabled = ledger.isAutonomyEnabled()
                val isStopped = ledger.isEmergencyStopActive()
                call.respond(mapOf(
                    "autonomyEnabled" to isEnabled,
                    "emergencyStop" to isStopped
                ))
            }
            
            // Capability Reality Matrix Endpoints
            get("/api/v1/capabilities") {
                val caps = ledger.getCapabilities()
                call.respond(caps)
            }
            post("/api/v1/capabilities/{id}/verify") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                val result = ledger.verifyCapability(id)
                call.respond(result)
            }
            post("/api/v1/capabilities/reality_sweep") {
                val report = ledger.runRealitySweep()
                call.respond(report)
            }
            get("/api/v1/capabilities/transitions") {
                val transitions = ledger.getCapabilityTransitions()
                call.respond(transitions)
            }
            post("/api/v1/capabilities/{id}/toggle") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                val body = try { call.receive<Map<String, Boolean>>() } catch (e: Exception) { mapOf("enabled" to true) }
                val enabled = body["enabled"] ?: true
                val result = ledger.toggleCapability(id, enabled)
                call.respond(result)
            }

            // Parallel Worker Streams & Active Cycle
            get("/api/v1/cycles/active") {
                val activeCycle = ledger.getActiveCycle()
                if (activeCycle != null) {
                    call.respond(activeCycle)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active cycle"))
                }
            }
            post("/api/v1/cycles/{cycleId}/cancel") {
                val cycleId = call.parameters["cycleId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing cycleId"))
                val success = ledger.cancelCycle(cycleId)
                call.respond(mapOf("cycleId" to cycleId, "cancelled" to success))
            }
            post("/api/v1/workers/{workerId}/cancel") {
                val workerId = call.parameters["workerId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val success = ledger.cancelWorker(workerId)
                call.respond(mapOf("workerId" to workerId, "cancelled" to success))
            }

            // Live Capability Telemetry
            get("/api/v1/telemetry") {
                val telemetry = ledger.getTelemetry()
                call.respond(telemetry)
            }

            // Tandem Co-Development
            get("/api/v1/tandem") {
                val tandem = ledger.getTandemDevelopment()
                call.respond(tandem)
            }
            post("/api/v1/development_signals") {
                val body = try { call.receive<Map<String, String>>() } catch (e: Exception) { emptyMap<String, String>() }
                val type = body["type"] ?: "NEW_REQUIREMENT"
                val project = body["project"] ?: "general"
                val intent = body["intent"] ?: "Owner requested action"
                val recorded = ledger.recordDevelopmentSignal(type, project, intent)
                call.respond(recorded)
            }

            
            // Real Worker Protocol Endpoints
            post("/api/v1/worker/enroll") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val caps = body["capabilities"] as? Map<String, Any> ?: emptyMap()
                val os = caps["os"] as? String ?: "unknown"
                val unrealVersion = caps["unrealVersion"] as? String ?: "unknown"
                val repository = caps["repository"] as? String ?: "unknown"
                val currentBranch = caps["currentBranch"] as? String ?: "unknown"
                val currentCommit = caps["currentCommit"] as? String ?: "unknown"
                
                val result = ledger.enrollWorker(workerId, os, unrealVersion, repository, currentBranch, currentCommit)
                call.respond(result)
            }

            post("/api/v1/worker/heartbeat") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val state = body["state"] as? String ?: "UNKNOWN"
                
                val result = ledger.heartbeatWorker(workerId, state)
                call.respond(result)
            }

            post("/api/v1/worker/jobs/lease") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                
                val job = ledger.leaseJob(workerId)
                if (job != null) {
                    call.respond(job)
                } else {
                    call.respond(HttpStatusCode.NoContent, mapOf("message" to "No jobs available"))
                }
            }

            post("/api/v1/worker/jobs/{jobId}/complete") {
                val jobId = call.parameters["jobId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing jobId"))
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                
                val exitStatus = (body["exitStatus"] as? Number)?.toInt() ?: 1
                val evidenceLevel = body["evidenceLevel"] as? String ?: "NONE"
                val stdout = body["stdout"] as? String ?: ""
                val stderr = body["stderr"] as? String ?: ""
                
                val success = ledger.completeJob(jobId, exitStatus, evidenceLevel, stdout, stderr)
                call.respond(mapOf("success" to success))
            }

            post("/api/v1/worker/artifacts") {
                // In a real implementation this would parse multipart/form-data for the file stream.
                // For architecture/protocol completeness, we assume a JSON payload describing the artifact and a mock upload, or base64.
                // To support a real test, let's accept a JSON payload with file content as base64, save it, and register it.
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                
                val jobId = body["jobId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing jobId"))
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val sha256 = body["sha256"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing sha256"))
                val size = (body["size"] as? Number)?.toLong() ?: 0L
                val path = body["path"] as? String ?: "unknown"
                val contentBase64 = body["contentBase64"] as? String
                
                // Store file physically in the library
                val artifactDir = java.io.File("/app/applet/library/artifacts")
                artifactDir.mkdirs()
                val artifactFile = java.io.File(artifactDir, sha256)
                
                if (contentBase64 != null) {
                    val decoded = java.util.Base64.getDecoder().decode(contentBase64)
                    artifactFile.writeBytes(decoded)
                } else {
                    artifactFile.writeText("empty artifact or multipart used in real environment")
                }
                
                val uri = "file://${artifactFile.absolutePath}"
                
                val result = ledger.registerArtifact(jobId, workerId, sha256, size, path, uri)
                call.respond(result)
            }

            // Governance Controls
            post("/api/v1/control_plane/pause") {
                ledger.setAutonomyEnabled(false)
                call.respond(mapOf("status" to "PAUSED"))
            }
            post("/api/v1/control_plane/resume") {
                ledger.setEmergencyStop(false)
                ledger.setAutonomyEnabled(true)
                call.respond(mapOf("status" to "RESUMED"))
            }
            post("/api/v1/control_plane/emergency_stop") {
                ledger.setEmergencyStop(true)
                call.respond(mapOf("status" to "EMERGENCY_STOP_ACTIVATED"))
            }
        }
    }.start(wait = false)
}


/**
 * Browser clients (the M. Engine web PWA) are served from a different origin than
 * this API, so without CORS every request from the browser is rejected before it
 * reaches a route.
 *
 * Origins are allow-listed rather than wildcarded. This API exposes governance
 * controls — pause, resume, emergency_stop — and currently has NO authentication,
 * so any origin permitted here can drive them. Widen it deliberately, not by
 * default.
 *
 * Configure with CORS_ALLOWED_ORIGINS as a comma-separated list of full origins,
 * e.g. "https://example.github.io,http://localhost:5173".
 */
private fun Application.installCors() {
    val configured = System.getenv("CORS_ALLOWED_ORIGINS")
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    val origins = if (configured.isNotEmpty()) configured else DEFAULT_ALLOWED_ORIGINS

    install(CORS) {
        origins.forEach { origin ->
            val scheme = origin.substringBefore("://", "https")
            val host = origin.substringAfter("://")
            if (host.isNotEmpty()) allowHost(host, schemes = listOf(scheme))
        }
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    println("CORS allowed origins: " + origins.joinToString(", "))
}

private val DEFAULT_ALLOWED_ORIGINS = listOf(
    "http://localhost:5173",
    "http://localhost:4173",
    "http://127.0.0.1:5173",
)
