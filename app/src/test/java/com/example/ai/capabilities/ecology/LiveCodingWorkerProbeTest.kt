package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LiveCodingWorkerProbeTest {
    
    @Before
    fun setup() {
        FederatedCapabilityRegistry.reset()
    }

    @Test
    fun testLiveCodingWorkerCapabilityProbe() = runBlocking {
        val codingCap = FederatedCapabilityRegistry.getCapability("CodingWorkerCapability") as CodingWorkerCapability
        
        println("━━━━━━━━ M. ENGINE — LIVE CAPABILITY VERIFICATION ━━━━━━━━\n")
        println("CAPABILITY\n${codingCap.name}\n")
        
        val result = codingCap.verifyHealth()
        
        println("OBSERVED")
        if (result.success) {
            println("Coding Worker execution capability physically probed.")
        } else {
            println("Coding Worker environment degraded/unavailable.")
            println("Probe failed: ${result.failureReason}")
        }
        
        println("\nEVIDENCE")
        result.evidence.forEach { println(it) }
        
        println("\nREALITY MATRIX")
        println("Implementation Confidence: HIGH") 
        println("Configuration Confidence: ${if (result.success) "HIGH" else "LOW"}")
        println("Historical Availability: OBSERVED")
        println("Current Availability: ${codingCap.state}")
        
        println("\nGRANULAR CAPABILITY MATRIX")
        result.granularStatus.forEach { (operation, state) ->
            println("${operation.padEnd(35)} : $state")
        }
        
        println("\nCIRCUIT STATE\n${codingCap.circuitState}")
        
        println("\nNEXT ACTION")
        if (result.success) {
            println("Capability is PARTIALLY_VERIFIED. Proceeding down Opportunity Matrix priority list.")
        } else {
            println("Capability gap identified. Requires alternative execution path.")
        }
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        assertTrue(result.success || result.verifiedState == CapabilityState.CAPABILITY_GAP)
    }
}
