package com.example.ai.capabilities

data class ImplementationRecord(
    val capabilityName: String,
    val implementationName: String,
    val sourceRepo: String,
    val benchmarkScore: Float,
    val evidenceLedgerId: String
)

data class IntegrationDecision(
    val shouldIntegrate: Boolean,
    val reason: String,
    val scoreDelta: Float
)

interface CapabilityHarvestMatrix {
    suspend fun getCurrentImplementation(capabilityName: String): ImplementationRecord?
    suspend fun registerCandidateEvaluation(capabilityName: String, record: ImplementationRecord)
    suspend fun compareCapabilities(current: ImplementationRecord?, candidate: ImplementationRecord): IntegrationDecision
}
