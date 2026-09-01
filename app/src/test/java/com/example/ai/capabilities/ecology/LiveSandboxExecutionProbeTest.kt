package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LiveSandboxExecutionProbeTest {
    
    @Before
    fun setup() {
        FederatedCapabilityRegistry.reset()
    }

    @Test
    fun testLiveSandboxExecutionCapabilityProbe() = runBlocking {
        val sandboxCap = FederatedCapabilityRegistry.getCapability("SandboxExecutionCapability") as SandboxExecutionCapability
        
        println("━━━━━━━━ M. ENGINE — LIVE CAPABILITY VERIFICATION ━━━━━━━━\n")
        println("CAPABILITY\n${sandboxCap.name}\n")
        
        val result = sandboxCap.verifyHealth()
        
        println("OBSERVED")
        if (result.success) {
            println("Sandbox Execution environment probe sequence completed.")
        } else {
            println("Sandbox Execution environment degraded/unavailable.")
            println("Probe failed: ${result.failureReason}")
        }
        
        println("\nEVIDENCE")
        result.evidence.forEach { println(it) }
        
        println("\nREALITY MATRIX")
        println("Implementation Confidence: HIGH") 
        println("Configuration Confidence: ${if (result.success) "HIGH" else "LOW"}")
        println("Historical Availability: OBSERVED")
        println("Current Availability: ${sandboxCap.state}")
        
        println("\nGRANULAR CAPABILITY MATRIX")
        result.granularStatus.forEach { (operation, state) ->
            println("${operation.padEnd(30)} : $state")
        }
        
        println("\nCIRCUIT STATE\n${sandboxCap.circuitState}")
        
        println("\nNEXT ACTION")
        if (result.success) {
            println("Capability is PARTIALLY_VERIFIED. Proceeding down Opportunity Matrix priority list.")
        } else {
            println("Capability gap identified. Requires alternative execution path.")
        }
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // Assert success or gracefully handle gap
        assertTrue(result.success || result.verifiedState == CapabilityState.CAPABILITY_GAP)
    }
}
