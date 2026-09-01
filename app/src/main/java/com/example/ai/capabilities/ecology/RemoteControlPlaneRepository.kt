package com.example.ai.capabilities.ecology

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class MindstreamEntry(
    val timestamp: String,
    val source: String = "REMOTE_GOVERNOR",
    val runId: String = "",
    val workerId: String = "",
    val category: String,
    val objective: String = "",
    val observation: String = "",
    val evidence: String = "",
    val decision: String = "",
    val result: String = "",
    val nextAction: String = "",
    val confidence: String = "",
    val authorizationLevel: String = ""
)

data class ControlPlaneStateResponse(
    val autonomyEnabled: Boolean,
    val emergencyStop: Boolean
)

interface ControlPlaneApi {
    @GET("/health")
    suspend fun getHealth(): Map<String, String>

    @GET("/ready")
    suspend fun getReady(): Map<String, String>

    @GET("/api/v1/mindstream")
    suspend fun getMindstream(): List<String>

    @GET("/api/v1/control_plane")
    suspend fun getControlPlaneState(): ControlPlaneStateResponse

    @GET("/api/v1/capabilities")
    suspend fun getCapabilities(): List<Map<String, Any>>

    @POST("/api/v1/capabilities/{id}/verify")
    suspend fun verifyCapability(@Path("id") id: String): Map<String, Any>

    @POST("/api/v1/capabilities/reality_sweep")
    suspend fun runRealitySweep(): Map<String, Any>

    @GET("/api/v1/capabilities/transitions")
    suspend fun getCapabilityTransitions(): List<Map<String, Any>>

    @POST("/api/v1/capabilities/{id}/toggle")
    suspend fun toggleCapability(@Path("id") id: String, @Body body: Map<String, Boolean>): Map<String, Any>

    @GET("/api/v1/cycles/active")
    suspend fun getActiveCycle(): Map<String, Any>

    @POST("/api/v1/cycles/{cycleId}/cancel")
    suspend fun cancelCycle(@Path("cycleId") cycleId: String): Map<String, Any>

    @POST("/api/v1/workers/{workerId}/cancel")
    suspend fun cancelWorker(@Path("workerId") workerId: String): Map<String, Any>

    @GET("/api/v1/telemetry")
    suspend fun getTelemetry(): Map<String, Any>

    @GET("/api/v1/tandem")
    suspend fun getTandemDevelopment(): Map<String, Any>

    @POST("/api/v1/development_signals")
    suspend fun sendDevelopmentSignal(@Body body: Map<String, String>): Map<String, Any>

    @POST("/api/v1/control_plane/emergency_stop")
    suspend fun triggerEmergencyStop(): Map<String, String>

    @POST("/api/v1/control_plane/resume")
    suspend fun resumeControlPlane(): Map<String, String>

    @POST("/api/v1/control_plane/pause")
    suspend fun pauseControlPlane(): Map<String, String>
}

enum class RemoteGovernorState {
    CONNECTED,
    DEGRADED,
    OFFLINE,
    LOCAL_FALLBACK,
    SYNCING
}

class RemoteControlPlaneRepository {
    private var currentBaseUrl: String = ""
    private var cachedApi: ControlPlaneApi? = null

    private fun getApi(): ControlPlaneApi {
        val activeUrl = RemoteEndpointConfiguration.getActiveUrl()
        if (cachedApi == null || currentBaseUrl != activeUrl) {
            currentBaseUrl = activeUrl
            cachedApi = Retrofit.Builder()
                .baseUrl(activeUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ControlPlaneApi::class.java)
        }
        return cachedApi!!
    }

    private val _connectionState = MutableStateFlow(RemoteGovernorState.OFFLINE)
    val connectionState: StateFlow<RemoteGovernorState> = _connectionState.asStateFlow()

    private val _controlPlaneState = MutableStateFlow<ControlPlaneStateResponse?>(null)
    val controlPlaneState: StateFlow<ControlPlaneStateResponse?> = _controlPlaneState.asStateFlow()

    private val _mindstream = MutableStateFlow<List<String>>(emptyList())
    val mindstream: StateFlow<List<String>> = _mindstream.asStateFlow()

    private val _capabilities = MutableStateFlow<List<CapabilityRuntimeState>>(FederatedCapabilityRegistry.getRuntimeStates())
    val capabilities: StateFlow<List<CapabilityRuntimeState>> = _capabilities.asStateFlow()

    private val _activeCycle = MutableStateFlow<AutonomousCycleState?>(null)
    val activeCycle: StateFlow<AutonomousCycleState?> = _activeCycle.asStateFlow()

    private val _telemetry = MutableStateFlow(
        CapabilityTelemetry(
            activeWorkers = 1,
            queuedWorkers = 1,
            completedWorkers = 4,
            failedWorkers = 0,
            averageExecutionTime = 135L,
            budgetConsumption = 0.015,
            capabilityAvailability = FederatedCapabilityRegistry.getRuntimeStates().associate { it.capabilityId to it.state },
            lastHeartbeat = System.currentTimeMillis()
        )
    )
    val telemetry: StateFlow<CapabilityTelemetry> = _telemetry.asStateFlow()

    private val _tandemSignals = MutableStateFlow<List<DevelopmentSignal>>(SharedDevelopmentMemory.signals.value)
    val tandemSignals: StateFlow<List<DevelopmentSignal>> = _tandemSignals.asStateFlow()

    private val _causalRecords = MutableStateFlow<List<CausalDevelopmentRecord>>(SharedDevelopmentMemory.causalRecords.value)
    val causalRecords: StateFlow<List<CausalDevelopmentRecord>> = _causalRecords.asStateFlow()

    private val _sweepReport = MutableStateFlow<RealitySweepReport?>(CapabilityRealitySweepEngine.lastSweepReport.value)
    val sweepReport: StateFlow<RealitySweepReport?> = _sweepReport.asStateFlow()

    private val _transitionHistory = MutableStateFlow<List<CapabilityTransitionRecord>>(CapabilityRealitySweepEngine.transitionHistory.value)
    val transitionHistory: StateFlow<List<CapabilityTransitionRecord>> = _transitionHistory.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        ConnectionDiagnostic(
            environment = RemoteEndpointConfiguration.selectedEnvironment.value,
            endpointUrl = RemoteEndpointConfiguration.getActiveUrl(),
            transportSecurity = RemoteEndpointConfiguration.getTransportSecurity(RemoteEndpointConfiguration.getActiveUrl()),
            activeGovernorState = RemoteGovernorState.OFFLINE
        )
    )
    val diagnostics: StateFlow<ConnectionDiagnostic> = _diagnostics.asStateFlow()

    suspend fun refreshState() {
        val activeUrl = RemoteEndpointConfiguration.getActiveUrl()
        val activeEnv = RemoteEndpointConfiguration.selectedEnvironment.value
        val secState = RemoteEndpointConfiguration.getTransportSecurity(activeUrl)

        _connectionState.value = RemoteGovernorState.SYNCING
        updateDiagnostics(activeEnv, activeUrl, secState, RemoteGovernorState.SYNCING)

        try {
            val apiInstance = getApi()
            val health = apiInstance.getHealth()
            if (health["status"] == "UP") {
                try {
                    val ready = apiInstance.getReady()
                    if (ready["status"] == "READY") {
                        val state = apiInstance.getControlPlaneState()
                        val stream = apiInstance.getMindstream()

                        _connectionState.value = RemoteGovernorState.CONNECTED
                        _controlPlaneState.value = state
                        _mindstream.value = stream

                        // Fetch capabilities & telemetry from remote
                        try {
                            val capMapList = apiInstance.getCapabilities()
                            val parsedCaps = capMapList.map { map ->
                                val stateStr = map["state"] as? String ?: "REGISTERED"
                                val st = runCatching { CapabilityState.valueOf(stateStr) }.getOrDefault(CapabilityState.REGISTERED)
                                CapabilityRuntimeState(
                                    capabilityId = map["capabilityId"] as? String ?: "unknown",
                                    capabilityType = map["capabilityType"] as? String ?: "unknown",
                                    registered = map["registered"] as? Boolean ?: true,
                                    configured = map["configured"] as? Boolean ?: true,
                                    authorized = map["authorized"] as? Boolean ?: true,
                                    available = map["available"] as? Boolean ?: false,
                                    state = st,
                                    lastHealthCheck = (map["lastHealthCheck"] as? Number)?.toLong(),
                                    currentWorkerCount = (map["currentWorkerCount"] as? Number)?.toInt() ?: 0,
                                    maximumWorkerCount = (map["maximumWorkerCount"] as? Number)?.toInt() ?: 3,
                                    costBudget = (map["costBudget"] as? Number)?.toDouble() ?: 1.0,
                                    remainingBudget = (map["remainingBudget"] as? Number)?.toDouble() ?: 1.0,
                                    environmentIdentity = map["environmentIdentity"] as? String ?: "remote-node",
                                    verificationEvidence = (map["verificationEvidence"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                                    isEnabled = map["isEnabled"] as? Boolean ?: true
                                )
                            }
                            if (parsedCaps.isNotEmpty()) {
                                _capabilities.value = parsedCaps
                            }
                        } catch (capErr: Exception) {
                            // keep local capabilities state
                        }

                        // Fetch telemetry
                        try {
                            val telemMap = apiInstance.getTelemetry()
                            _telemetry.value = CapabilityTelemetry(
                                activeWorkers = (telemMap["activeWorkers"] as? Number)?.toInt() ?: 0,
                                queuedWorkers = (telemMap["queuedWorkers"] as? Number)?.toInt() ?: 0,
                                completedWorkers = (telemMap["completedWorkers"] as? Number)?.toInt() ?: 0,
                                failedWorkers = (telemMap["failedWorkers"] as? Number)?.toInt() ?: 0,
                                averageExecutionTime = (telemMap["averageExecutionTime"] as? Number)?.toLong() ?: 0L,
                                budgetConsumption = (telemMap["budgetConsumption"] as? Number)?.toDouble() ?: 0.0,
                                capabilityAvailability = _capabilities.value.associate { it.capabilityId to it.state },
                                lastHeartbeat = System.currentTimeMillis()
                            )
                        } catch (tErr: Exception) {
                            // telemetry fallback
                        }

                        _diagnostics.value = ConnectionDiagnostic(
                            environment = activeEnv,
                            endpointUrl = activeUrl,
                            transportSecurity = secState,
                            lastSuccessfulHeartbeat = System.currentTimeMillis(),
                            lastFailure = null,
                            lastFailureTimestamp = _diagnostics.value.lastFailureTimestamp,
                            activeGovernorState = RemoteGovernorState.CONNECTED
                        )
                    } else {
                        _connectionState.value = RemoteGovernorState.DEGRADED
                        updateDiagnostics(activeEnv, activeUrl, secState, RemoteGovernorState.DEGRADED, failure = "Ready check returned non-ready status")
                    }
                } catch (readyErr: Exception) {
                    _connectionState.value = RemoteGovernorState.DEGRADED
                    updateDiagnostics(activeEnv, activeUrl, secState, RemoteGovernorState.DEGRADED, failure = "Database / ready check failed: ${readyErr.localizedMessage}")
                }
            } else {
                _connectionState.value = RemoteGovernorState.DEGRADED
                updateDiagnostics(activeEnv, activeUrl, secState, RemoteGovernorState.DEGRADED, failure = "Health check returned non-UP status")
            }
        } catch (e: Exception) {
            _connectionState.value = RemoteGovernorState.LOCAL_FALLBACK
            updateDiagnostics(activeEnv, activeUrl, secState, RemoteGovernorState.LOCAL_FALLBACK, failure = e.localizedMessage ?: "Connection refused / host unreachable")
            
            // Fallback to local state
            _capabilities.value = FederatedCapabilityRegistry.getRuntimeStates()
            _tandemSignals.value = SharedDevelopmentMemory.signals.value
            _causalRecords.value = SharedDevelopmentMemory.causalRecords.value
            _mindstream.value = SharedDevelopmentMemory.mindstream.value
            _telemetry.value = _telemetry.value.copy(
                capabilityAvailability = FederatedCapabilityRegistry.getRuntimeStates().associate { it.capabilityId to it.state },
                lastHeartbeat = System.currentTimeMillis()
            )
        }
    }

    private fun updateDiagnostics(
        env: EndpointEnvironment,
        url: String,
        security: TransportSecurityState,
        governorState: RemoteGovernorState,
        failure: String? = null
    ) {
        _diagnostics.value = ConnectionDiagnostic(
            environment = env,
            endpointUrl = url,
            transportSecurity = security,
            lastSuccessfulHeartbeat = _diagnostics.value.lastSuccessfulHeartbeat,
            lastFailure = failure ?: _diagnostics.value.lastFailure,
            lastFailureTimestamp = if (failure != null) System.currentTimeMillis() else _diagnostics.value.lastFailureTimestamp,
            activeGovernorState = governorState
        )
    }

    fun switchEnvironment(env: EndpointEnvironment) {
        RemoteEndpointConfiguration.setEnvironment(env)
        currentBaseUrl = ""
    }

    fun setCustomEndpoint(url: String) {
        RemoteEndpointConfiguration.setCustomUrl(url)
        currentBaseUrl = ""
    }

    suspend fun verifyCapability(capabilityId: String): HealthCheckResult {
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                val res = getApi().verifyCapability(capabilityId)
                val success = res["success"] as? Boolean ?: false
                val evidence = (res["evidence"] as? List<*>)?.map { it.toString() } ?: emptyList()
                refreshState()
                return HealthCheckResult(
                    capabilityId = capabilityId,
                    success = success,
                    latencyMs = 45L,
                    evidence = evidence,
                    verifiedState = if (success) CapabilityState.AVAILABLE else CapabilityState.FAILED
                )
            } catch (e: Exception) {
                // fallback to local verification
            }
        }
        val result = FederatedCapabilityRegistry.verifyCapability(capabilityId)
            ?: HealthCheckResult(
                capabilityId = capabilityId,
                success = false,
                latencyMs = 0L,
                evidence = emptyList(),
                verifiedState = CapabilityState.FAILED,
                failureReason = "Capability not registered"
            )
        _capabilities.value = FederatedCapabilityRegistry.getRuntimeStates()
        return result
    }

    suspend fun verifyAllCapabilities(): List<HealthCheckResult> {
        val results = mutableListOf<HealthCheckResult>()
        for (cap in FederatedCapabilityRegistry.getAllCapabilities()) {
            results.add(verifyCapability(cap.capabilityId))
        }
        _transitionHistory.value = CapabilityRealitySweepEngine.transitionHistory.value
        return results
    }

    suspend fun runCapabilityRealitySweep(): RealitySweepReport {
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                getApi().runRealitySweep()
            } catch (e: Exception) {
                // fallback to local engine
            }
        }
        val report = CapabilityRealitySweepEngine.executeSweep()
        _sweepReport.value = report
        _transitionHistory.value = CapabilityRealitySweepEngine.transitionHistory.value
        _capabilities.value = FederatedCapabilityRegistry.getRuntimeStates()
        return report
    }

    suspend fun toggleCapability(capabilityId: String, enabled: Boolean) {
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                getApi().toggleCapability(capabilityId, mapOf("enabled" to enabled))
            } catch (e: Exception) {
                // ignore
            }
        }
        FederatedCapabilityRegistry.toggleCapability(capabilityId, enabled)
        _capabilities.value = FederatedCapabilityRegistry.getRuntimeStates()
    }

    suspend fun cancelCycle(cycleId: String) {
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                getApi().cancelCycle(cycleId)
            } catch (e: Exception) {
                // ignore
            }
        }
        _activeCycle.value = _activeCycle.value?.copy(status = "CANCELLED", exitReason = "CANCELLED_BY_OWNER")
        refreshState()
    }

    suspend fun cancelWorker(workerId: String) {
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                getApi().cancelWorker(workerId)
            } catch (e: Exception) {
                // ignore
            }
        }
        val current = _activeCycle.value
        if (current != null) {
            current.workerJobs.find { it.workerId == workerId }?.let {
                it.state = WorkerJobState.CANCELLED
                it.failureReason = "Cancelled by owner"
                it.completedAt = System.currentTimeMillis()
            }
            _activeCycle.value = current.copy()
        }
    }

    suspend fun sendDevelopmentSignal(type: DevelopmentSignalType, project: String, intent: String) {
        val signal = DevelopmentSignal(
            type = type,
            project = project,
            intent = intent,
            status = SignalStatus.RECEIVED
        )
        SharedDevelopmentMemory.ingestSignal(signal)
        _tandemSignals.value = SharedDevelopmentMemory.signals.value
        if (_connectionState.value == RemoteGovernorState.CONNECTED) {
            try {
                getApi().sendDevelopmentSignal(
                    mapOf(
                        "type" to type.name,
                        "project" to project,
                        "intent" to intent
                    )
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        refreshState()
    }

    suspend fun emergencyStop() {
        try {
            getApi().triggerEmergencyStop()
            refreshState()
        } catch (e: Exception) {
            AutonomyControlPlane.triggerEmergencyStop()
            _controlPlaneState.value = ControlPlaneStateResponse(autonomyEnabled = true, emergencyStop = true)
        }
    }

    suspend fun resume() {
        try {
            getApi().resumeControlPlane()
            refreshState()
        } catch (e: Exception) {
            AutonomyControlPlane.resumeExecution()
            _controlPlaneState.value = ControlPlaneStateResponse(autonomyEnabled = true, emergencyStop = false)
        }
    }

    suspend fun pause() {
        try {
            getApi().pauseControlPlane()
            refreshState()
        } catch (e: Exception) {
            AutonomyControlPlane.pauseExecution()
            _controlPlaneState.value = ControlPlaneStateResponse(autonomyEnabled = false, emergencyStop = false)
        }
    }
}
