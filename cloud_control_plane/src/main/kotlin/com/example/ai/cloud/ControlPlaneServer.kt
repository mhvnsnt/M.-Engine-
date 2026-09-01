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
