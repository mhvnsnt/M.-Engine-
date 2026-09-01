package com.example.ai.capabilities.ecology

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Capability Reality Sweep Engine
 * 
 * Implements the Opportunity Engine priority ranking formula:
 * Score = (Owner relevance × Architectural dependency × Capability uncertainty × Ease of verification) ÷ (Verification cost + Risk)
 * 
 * Enforces the M. Engine Epistemic Reality Contract:
 * - Software verification ≠ Physical deployment reality.
 * - Capabilities are never claimed as VERIFIED_OPERATIONAL unless probed with timestamped physical evidence.
 * - Low-risk, high-leverage physical probes are executed in priority order when the task queue is empty.
 */
object CapabilityRealitySweepEngine {

    private val _lastSweepReport = MutableStateFlow<RealitySweepReport?>(null)
    val lastSweepReport: StateFlow<RealitySweepReport?> = _lastSweepReport.asStateFlow()

    private val _transitionHistory = MutableStateFlow<List<CapabilityTransitionRecord>>(emptyList())
    val transitionHistory: StateFlow<List<CapabilityTransitionRecord>> = _transitionHistory.asStateFlow()

    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping.asStateFlow()

    /**
     * Ranks all registered capabilities based on the Opportunity Engine priority equation.
     */
    fun computeRankings(capabilities: List<AgencyCapability> = FederatedCapabilityRegistry.getAllCapabilities()): List<CapabilityRealityRankingItem> {
        val scoredItems = capabilities.map { cap ->
            CapabilityRealityRankingItem(
                capabilityId = cap.capabilityId,
                capabilityType = cap.capabilityType,
                currentState = cap.state,
                score = cap.scoreConfig,
                recommendedProbeType = cap.probeType
            )
        }
        // Rank descending by score
        return scoredItems.sortedByDescending { it.score.score }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    /**
     * Executes the Capability Reality Sweep across all registered capabilities in priority order.
     */
    suspend fun executeSweep(): RealitySweepReport {
        if (_isSweeping.value) {
            return _lastSweepReport.value ?: RealitySweepReport(summary = "Sweep already in progress")
        }

        _isSweeping.value = true
        val sweepId = "sweep-${UUID.randomUUID().toString().take(8)}"
        val startTime = System.currentTimeMillis()
        val allCaps = FederatedCapabilityRegistry.getAllCapabilities()
        val rankedItems = computeRankings(allCaps)
        
        val transitions = mutableListOf<CapabilityTransitionRecord>()
        var verifiedCount = 0
        var physicallyAvailableCount = 0
        var unverifiedCount = 0
        var gapCount = 0

        // Ingest initial Mindstream observation conforming to Directed Initiative Loop
        SharedDevelopmentMemory.ingestMindstream(
            "CAPABILITY_REALITY_SWEEP: Starting sweep $sweepId across ${allCaps.size} registered capabilities ranked by Opportunity Engine formula."
        )

        for (ranked in rankedItems) {
            val cap = FederatedCapabilityRegistry.getCapability(ranked.capabilityId)
            if (cap == null || !cap.isEnabled) {
                continue
            }

            // Execute low-risk physical probe
            val probeResult = cap.verifyHealth()
            probeResult.transitionRecord?.let { trans ->
                transitions.add(trans)
            }

            when (cap.state) {
                CapabilityState.VERIFIED_OPERATIONAL, CapabilityState.AVAILABLE -> {
                    verifiedCount++
                    physicallyAvailableCount++
                }
                CapabilityState.PHYSICALLY_AVAILABLE -> {
                    physicallyAvailableCount++
                }
                CapabilityState.IMPLEMENTED_UNVERIFIED -> {
                    unverifiedCount++
                }
                CapabilityState.CAPABILITY_GAP, CapabilityState.FAILED, CapabilityState.UNAVAILABLE -> {
                    gapCount++
                }
                else -> {}
            }

            // Log individual probe observation
            SharedDevelopmentMemory.ingestMindstream(
                "PROBE_RECORD: [${cap.capabilityId}] Rank #${ranked.rank} (Score: ${"%.2f".format(ranked.score.score)}) -> ${cap.state.name} (${probeResult.latencyMs}ms, ${probeResult.evidence.size} physical proofs)"
            )
        }

        val allTransitions = _transitionHistory.value + transitions
        _transitionHistory.value = allTransitions.takeLast(100)

        val summary = "Reality Sweep $sweepId completed in ${System.currentTimeMillis() - startTime}ms. " +
                "Verified Operational: $verifiedCount/${allCaps.size}, Physically Available: $physicallyAvailableCount, " +
                "Unverified: $unverifiedCount, Gaps/Unavailable: $gapCount."

        val report = RealitySweepReport(
            sweepId = sweepId,
            timestamp = System.currentTimeMillis(),
            rankings = rankedItems,
            transitionsExecuted = transitions,
            verifiedOperationalCount = verifiedCount,
            physicalAvailableCount = physicallyAvailableCount,
            unverifiedCount = unverifiedCount,
            capabilityGapCount = gapCount,
            totalCapabilities = allCaps.size,
            summary = summary
        )

        _lastSweepReport.value = report
        _isSweeping.value = false

        SharedDevelopmentMemory.ingestMindstream("CAPABILITY_REALITY_SWEEP_COMPLETE: $summary")
        return report
    }
}
