package com.example.ai.capabilities.ecology

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * MISSION 17.2C - The Observatory Contract
 * 
 * This API definition represents the boundary where the Android application
 * transitions from being the "Brain" to being the "Cockpit / Observatory".
 * 
 * Android will use this to synchronize with the persistent Cloud Control Plane (PostgreSQL).
 */
interface RemoteAgencyLedgerApi {

    // --- Control Plane (Kill Switches & Autonomy State) ---
    @GET("/api/v1/control_plane")
    suspend fun getControlPlaneState(): Response<ControlPlaneStateDto>

    @PUT("/api/v1/control_plane/emergency_stop")
    suspend fun triggerEmergencyStop(@Body request: EmergencyStopRequest): Response<Unit>

    // --- Human Development Signals ---
    @POST("/api/v1/signals")
    suspend fun emitDevelopmentSignal(@Body signal: DevelopmentSignalDto): Response<Unit>

    // --- Observatory / Mindstream ---
    @GET("/api/v1/mindstream")
    suspend fun getMindstream(
        @retrofit2.http.Query("limit") limit: Int = 50,
        @retrofit2.http.Query("offset") offset: Int = 0
    ): Response<List<MindstreamEntryDto>>

    // --- Epistemic Memory & Evidence ---
    @GET("/api/v1/evidence/active")
    suspend fun getActiveOpportunitiesAndEvidence(): Response<List<OpportunityWithEvidenceDto>>
}

// Data Transfer Objects (DTOs)
data class ControlPlaneStateDto(
    val autonomyEnabled: Boolean,
    val emergencyStop: Boolean
)

data class EmergencyStopRequest(
    val reason: String,
    val authorizedBy: String = "OWNER"
)

data class DevelopmentSignalDto(
    val signalType: String,
    val intent: String
)

data class MindstreamEntryDto(
    val id: String,
    val entryType: String, // OBSERVED, INFERENCE, INTENT, EXPERIMENT, RESULT, DECISION
    val content: String,
    val timestamp: String
)

data class OpportunityWithEvidenceDto(
    val id: String,
    val description: String,
    val priorityScore: Int,
    val status: String,
    val evidence: List<String>
)
