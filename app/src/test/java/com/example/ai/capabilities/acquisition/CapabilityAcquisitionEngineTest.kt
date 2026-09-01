package com.example.ai.capabilities.acquisition

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class CapabilityAcquisitionEngineTest {

    @Test
    fun testOpenHandsZeroConfigDiscovery() = runBlocking {
        val manager = CapabilityAcquisitionManager()
        
        // 1. Missing credentials -> should NOT fail, should log CAPABILITY_GAP
        val result = manager.discoverOpenHandsRuntime(null, null)
        
        assertEquals(DiscoveryState.RUNTIME_NOT_PROVISIONED, result.discoveryState)
        assertEquals("NOT EVALUATED", result.authenticationState)
        
        result.printLedger()
    }
    
    @Test
    fun executePhysicalProbes() = runBlocking {
        val discovery = PhysicalRuntimeDiscovery()
        val observations = discovery.executeProbes()
        
        observations.forEach { it.printLedger() }
    }
}
