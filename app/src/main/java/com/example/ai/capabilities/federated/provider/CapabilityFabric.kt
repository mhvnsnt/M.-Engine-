package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Capability Fabric — the bootstrap that makes the federated provider layer real.
 *
 * The providers (OpenHands, Hatchet, LiteLLM, Playwright, MinIO, Postgres) were
 * already written, already speak real HTTP, and already report CAPABILITY_GAP
 * honestly when their backend is absent. What did not exist was anything that
 * CONSTRUCTED them: a static reachability audit found the whole
 * federated/provider package unreferenced from any entry point, so none of it
 * could ever run.
 *
 * This class is that missing connection. It owns the registry, holds the
 * endpoint configuration, probes every provider against its real backend, and
 * publishes the result for the UI.
 *
 * It deliberately does NOT fabricate availability. A provider whose backend is
 * not running reports UNAVAILABLE with the reason, which is the state
 * REALITY_CONTRACT.md calls BLOCKED_BY_EXTERNAL_DEPENDENCY. That is the honest
 * answer on a phone with no local OpenHands, and it is what tells the owner
 * exactly which runtime to stand up next.
 */
class CapabilityFabric(
    endpoints: FabricEndpoints = FabricEndpoints(),
) {
    val registry = ProviderRegistry()

    private val _catalog = MutableStateFlow<List<CapabilityCatalogEntry>>(emptyList())

    /** Latest probe results. Empty until [probeAll] has run at least once. */
    val catalog: StateFlow<List<CapabilityCatalogEntry>> = _catalog.asStateFlow()

    private val _probing = MutableStateFlow(false)
    val probing: StateFlow<Boolean> = _probing.asStateFlow()

    init {
        registry.register(OpenHandsCodingProvider(OpenHandsClient(endpoints.openHands)))
        registry.register(HatchetWorkflowProvider(HatchetClient(endpoints.hatchet)))
        registry.register(LiteLLMModelProvider(LiteLLMClient(endpoints.liteLlm)))
        registry.register(PlaywrightBrowserProvider(PlaywrightClient(endpoints.playwright)))
        registry.register(MinIOStorageProvider(MinIOClient(endpoints.minio)))
        registry.register(PostgresDatabaseProvider(PostgresClient(endpoints.postgresHost, endpoints.postgresPort)))
        registry.register(
            UnrealExecutionProvider(
                UnrealWorkerClient(endpoints.unrealWorker, endpoints.unrealWorkerToken),
            ),
        )

        // The native fallback runs in-process, so it is the one provider that is
        // available with no external runtime at all.
        registry.register(NativeFallbackProvider())
    }

    /**
     * Probes every registered provider concurrently and republishes the catalog.
     *
     * Probes run in parallel because they are independent network round-trips
     * with their own timeouts; serially, seven unreachable backends would take
     * seven timeouts to report.
     */
    suspend fun probeAll(): List<CapabilityCatalogEntry> = withContext(Dispatchers.IO) {
        _probing.value = true
        try {
            val providers = registry.providers.value.values.toList()
            val results = coroutineScope {
                providers.map { provider ->
                    async {
                        val startedAt = System.currentTimeMillis()
                        val result = try {
                            provider.probe()
                        } catch (e: Exception) {
                            // A provider that throws is unavailable, not a crash
                            // of the fabric. Record why.
                            CapabilityProbeResult(
                                status = FabricNodeState.UNAVAILABLE,
                                error = "PROBE_FAILED: ${e.message ?: e::class.java.simpleName}",
                            )
                        }
                        CapabilityCatalogEntry(
                            providerId = provider.providerId,
                            capabilityType = provider.capabilityType,
                            status = result.status,
                            details = result.details,
                            error = result.error,
                            probeDurationMs = System.currentTimeMillis() - startedAt,
                            probedAt = System.currentTimeMillis(),
                        )
                    }
                }.awaitAll()
            }.sortedBy { it.capabilityType.name }

            _catalog.value = results
            results
        } finally {
            _probing.value = false
        }
    }

    /** Providers of [type] whose most recent probe found them actually available. */
    fun availableProviders(type: CapabilityType): List<CapabilityProvider> {
        val healthy = _catalog.value
            .filter { it.capabilityType == type && it.status == FabricNodeState.AVAILABLE }
            .map { it.providerId }
            .toSet()
        return registry.getProvidersByType(type).filter { it.providerId in healthy }
    }
}

/**
 * Endpoint configuration for the federated backends.
 *
 * Defaults are the projects' own documented local ports, so a self-hosted stack
 * on the same machine is discovered with no configuration. Anything else is the
 * owner's to point at — these are not secrets and are safe to persist.
 */
data class FabricEndpoints(
    val openHands: String = "http://localhost:3000",
    val hatchet: String = "http://localhost:8080",
    val liteLlm: String = "http://localhost:4000",
    val playwright: String = "http://localhost:8081",
    val minio: String = "http://localhost:9000",
    val postgresHost: String = "localhost",
    val postgresPort: Int = 5432,
    /**
     * Unreal runs on a separate machine by necessity — the engine is
     * licence-gated and far too large for a phone or a container.
     */
    val unrealWorker: String = "http://localhost:8770",
    val unrealWorkerToken: String = "",
)

/**
 * One row of the Capability Catalog: what a provider is, and what the last real
 * probe of its backend actually found.
 */
data class CapabilityCatalogEntry(
    val providerId: String,
    val capabilityType: CapabilityType,
    val status: FabricNodeState,
    val details: Map<String, String> = emptyMap(),
    val error: String? = null,
    val probeDurationMs: Long = 0,
    val probedAt: Long = 0,
) {
    /**
     * Maps a probe result onto the REALITY_CONTRACT.md vocabulary, so the UI
     * shows the same states the contract is written in.
     */
    val realityState: String
        get() = when (status) {
            FabricNodeState.AVAILABLE -> "REAL_AND_CONNECTED"
            FabricNodeState.PARTIALLY_VERIFIED -> "PARTIAL_REAL_IMPLEMENTATION"
            FabricNodeState.RELIABILITY_UNDER_OBSERVATION -> "REAL_BUT_UNVERIFIED"
            FabricNodeState.PROBING, FabricNodeState.DISCOVERED -> "REAL_BUT_UNVERIFIED"
            FabricNodeState.UNAVAILABLE -> "BLOCKED_BY_EXTERNAL_DEPENDENCY"
        }
}
