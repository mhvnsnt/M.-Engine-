package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalBrowserModelIntegrationTest {

    @Test
    fun `test physical LiteLLM connectivity according to Reality Contract`() = runBlocking {
        val client = LiteLLMClient("http://localhost:4000")
        val provider = LiteLLMModelProvider(client)

        val probeResult = provider.probe()
        
        println("EVIDENCE: LiteLLM Probe Status -> ${probeResult.status}")
        if (probeResult.error != null) {
            println("EVIDENCE: LiteLLM Probe Error -> ${probeResult.error}")
        }
        
        // We expect UNAVAILABLE because LiteLLM is not physically running on 4000 in this Sandbox
        assertEquals(FabricNodeState.UNAVAILABLE, probeResult.status)
        assertTrue(probeResult.error!!.contains("CAPABILITY_GAP"))
    }
    
    @Test
    fun `test physical Playwright connectivity according to Reality Contract`() = runBlocking {
        val client = PlaywrightClient("http://localhost:8081")
        val provider = PlaywrightBrowserProvider(client)

        val probeResult = provider.probe()
        
        println("EVIDENCE: Playwright Probe Status -> ${probeResult.status}")
        if (probeResult.error != null) {
            println("EVIDENCE: Playwright Probe Error -> ${probeResult.error}")
        }
        
        // We expect UNAVAILABLE because Playwright is not physically running on 8081 in this Sandbox
        assertEquals(FabricNodeState.UNAVAILABLE, probeResult.status)
        assertTrue(probeResult.error!!.contains("CAPABILITY_GAP"))
    }
}
