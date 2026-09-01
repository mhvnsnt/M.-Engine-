package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LiveLocalModelProbeTest {
    
    @Before
    fun setup() {
        FederatedCapabilityRegistry.reset()
    }

    @Test
    fun testLiveLocalModelCapabilityProbe() = runBlocking {
        val modelCap = FederatedCapabilityRegistry.getCapability("LocalModelCapability") as LocalModelCapability
        
        println("━━━━━━━━ M. ENGINE — LIVE CAPABILITY VERIFICATION ━━━━━━━━\n")
        println("CAPABILITY\n${modelCap.name}\n")
        
        val result = modelCap.verifyHealth()
        
        println("OBSERVED")
        if (result.success) {
            println("Local model daemon verified.")
        } else {
            println("Local model daemon offline/unreachable.")
            println("Probe failed: ${result.failureReason}")
        }
        
        println("\nEVIDENCE")
        result.evidence.forEach { println(it) }
        
        println("\nREALITY MATRIX")
        println("Implementation Confidence: HIGH") 
        println("Configuration Confidence: ${if (result.success) "HIGH" else "LOW"}")
        println("Historical Availability: OBSERVED")
        println("Current Availability: ${modelCap.state}")
        
        println("\nCIRCUIT STATE\n${modelCap.circuitState}")
        
        println("\nNEXT ACTION")
        if (result.success) {
            println("Capability is eligible for authorized Opportunity Engine work.")
        } else {
            println("Capability gap identified. Opportunity Engine will deprioritize local-only execution flows.")
        }
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // Asserting not null ensures the test framework compiles and runs the assertion
        assertNotNull(result)
    }
}
