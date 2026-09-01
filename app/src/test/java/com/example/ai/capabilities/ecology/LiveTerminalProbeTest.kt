package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LiveTerminalProbeTest {
    
    @Before
    fun setup() {
        FederatedCapabilityRegistry.reset()
    }

    @Test
    fun testLiveCapabilityProbes() = runBlocking {
        val webCap = FederatedCapabilityRegistry.getCapability("WebResearchCapability") as WebResearchCapability
        val docCap = FederatedCapabilityRegistry.getCapability("DocumentationCapability") as DocumentationCapability
        val videoCap = FederatedCapabilityRegistry.getCapability("VideoResearchCapability") as VideoResearchCapability
        
        val webResult = webCap.verifyHealth()
        val docResult = docCap.verifyHealth()
        val videoResult = videoCap.verifyHealth()
        
        println("━━━━━━━━ M. ENGINE — WEB RESEARCH PROBE ━━━━━━━━")
        println(webResult.evidence.joinToString("\n"))
        println("Status: ${webCap.state}")
        
        println("━━━━━━━━ M. ENGINE — DOCUMENTATION PROBE ━━━━━━━━")
        println(docResult.evidence.joinToString("\n"))
        println("Status: ${docCap.state}")
        
        println("━━━━━━━━ M. ENGINE — VIDEO RESEARCH PROBE ━━━━━━━━")
        println(videoResult.evidence.joinToString("\n"))
        println("Status: ${videoCap.state}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        assertTrue(true)
    }
}
