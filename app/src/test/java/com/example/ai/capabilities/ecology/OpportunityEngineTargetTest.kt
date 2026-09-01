package com.example.ai.capabilities.ecology

import org.junit.Test

class OpportunityEngineTargetTest {
    @Test
    fun testSelectNextTarget() {
        FederatedCapabilityRegistry.reset()
        // Simulate GitHub as AVAILABLE from the previous probe
        val github = FederatedCapabilityRegistry.getCapability("GitHubWorkerCapability") as GitHubWorkerCapability
        github.state = CapabilityState.AVAILABLE
        
        val rankings = CapabilityRealitySweepEngine.computeRankings(FederatedCapabilityRegistry.getAllCapabilities())
        
        println("━━━━━━━━ OPPORTUNITY ENGINE RANKING ━━━━━━━━")
        rankings.forEach {
            println("${it.rank}. ${it.capabilityType} (Score: ${it.score.score.format(2)}) - State: ${it.currentState}")
        }
        
        val nextTarget = rankings.firstOrNull { it.currentState == CapabilityState.IMPLEMENTED_UNVERIFIED || it.currentState == CapabilityState.DEGRADED }
        println("\nNEXT AUTONOMOUS TARGET: ${nextTarget?.capabilityType}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    private fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
}
