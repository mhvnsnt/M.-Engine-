package com.example.ai.capabilities

import android.util.Log

enum class RealityClassification {
    STUB,
    MOCK,
    SIMULATION,
    PARTIAL_REAL_IMPLEMENTATION,
    BLOCKED_BY_EXTERNAL_DEPENDENCY,
    REAL_BUT_UNVERIFIED,
    REAL_BUT_UNCONFIGURED,
    REAL_AND_CONNECTED
}

object RealityPolicy {
    const val REALITY_MODE_DEFAULT = "REAL"

    // M. ENGINE REALITY INVARIANT:
    // Never claim a capability exists merely because code representing the capability exists.
    // Never substitute a simulated capability for a requested real capability without explicit user authorization.
    // When reality cannot be accessed, report the limitation rather than fabricating reality.

    fun evaluateCapability(currentClassification: RealityClassification, dependencyAvailable: Boolean, hasVerifiedEvidence: Boolean, isConfigured: Boolean): RealityClassification {
        if (!dependencyAvailable && currentClassification != RealityClassification.SIMULATION && currentClassification != RealityClassification.MOCK) {
            Log.w("RealityPolicy", "Dependency unavailable. Marking BLOCKED_BY_EXTERNAL_DEPENDENCY.")
            return RealityClassification.BLOCKED_BY_EXTERNAL_DEPENDENCY
        }
        
        if (!isConfigured && currentClassification >= RealityClassification.REAL_BUT_UNCONFIGURED) {
            return RealityClassification.REAL_BUT_UNCONFIGURED
        }
        
        if (currentClassification == RealityClassification.REAL_AND_CONNECTED && !hasVerifiedEvidence) {
            Log.e("RealityPolicy", "Cannot claim REAL_AND_CONNECTED without physical evidence. Demoting.")
            return RealityClassification.REAL_BUT_UNVERIFIED
        }
        
        return currentClassification
    }
    
    /**
     * Evidence Engine enforcement rule.
     */
    fun canPromoteToProduction(classification: RealityClassification): Boolean {
        return when (classification) {
            RealityClassification.STUB,
            RealityClassification.MOCK,
            RealityClassification.SIMULATION,
            RealityClassification.PARTIAL_REAL_IMPLEMENTATION -> false
            else -> true
        }
    }
}

object SimulationDetector {
    private val simulationKeywords = listOf(
        "Mock", "Fake", "Stub", "Dummy", "NoOp", "Placeholder", 
        "TODO", "simulated", "simulation", "not implemented", 
        "future implementation", "sample data", "fake API"
    )

    fun analyzeImplementation(code: String, filePath: String): Boolean {
        // Legitimate tests are allowed to use mocks.
        if (filePath.contains("test", ignoreCase = true) || filePath.contains("androidTest", ignoreCase = true)) {
            return false
        }
        return simulationKeywords.any { code.contains(it, ignoreCase = true) }
    }
}
