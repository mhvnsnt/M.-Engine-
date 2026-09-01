package com.example.ai.capabilities.federated

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class LiveCodingRealityTrialTest {

    @Test
    fun testFirstBoundedCodingTrial_OrchestratorLogicVerified() = runBlocking {
        val adapter = OpenHandsWorkerAdapter()
        val orchestrator = LiveCodingRealityOrchestrator(adapter)
        
        val auth = CodingTrialAuthorization(
            repositoryId = "owner/test-repo",
            task = TrialTask(
                instruction = "Add one narrowly scoped regression test covering an already-existing behavior."
            )
        )
        
        val evidence = orchestrator.executeTrial(auth)
        
        // Assert Independent Verification Overrides Worker
        assertEquals(TrialState.VERIFIED_PARTIAL, evidence.finalState)
        assertNotNull(evidence.diffHash)
        
        // Ensure invariant compliance: Orchestrator is logic-verified, not live worker yet.
        assertEquals(EpistemicCapabilityState.ORCHESTRATOR_LOGIC_VERIFIED, evidence.capabilityState)
        
        assertTrue(evidence.stateProgression.contains(TrialState.REPOSITORY_OBSERVED))
        assertTrue(evidence.stateProgression.contains(TrialState.ARTIFACTS_INDEPENDENTLY_INSPECTED))
        assertTrue(evidence.stateProgression.contains(TrialState.CLEANUP_INSPECTED))
        
        evidence.printEvidenceLedger()
    }
    
    @Test
    fun testCapabilityProbe() = runBlocking {
        val probe = OpenHandsCapabilityProbe()
        
        // 1. Missing credentials => IMPLEMENTED_UNVERIFIED
        val missingResult = probe.executeProbe(null, null)
        assertEquals(EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED, missingResult)
        
        // 2. Valid mock credentials => PARTIALLY_VERIFIED
        val validResult = probe.executeProbe("https://openhands.internal", "mock-key")
        assertEquals(EpistemicCapabilityState.PARTIALLY_VERIFIED, validResult)
    }
}
