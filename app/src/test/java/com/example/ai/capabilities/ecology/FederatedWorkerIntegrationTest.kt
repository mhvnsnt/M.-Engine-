package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class FederatedWorkerIntegrationTest {
    
    @Test
    fun testNativeSandboxWorkerPhysicalDispatch() = runBlocking {
        val broker = FederatedWorkerBroker()
        
        val job = PhysicalWorkerJob(
            workerType = ExternalWorkerType.NATIVE_SANDBOX,
            targetRepository = "test-repo",
            instruction = "Create a test file and verify sandbox execution.",
            workspaceIsolated = true
        )
        
        val result = broker.submitJob(job)
        
        println("━━━━━━━━ M. ENGINE — PHYSICAL WORKER PROBE ━━━━━━━━")
        println("Job ID: ${result.jobId}")
        println("Exit Code: ${result.exitCode}")
        println("Diff: ${result.diff}")
        println("Stdout:\n${result.stdout}")
        println("Evidence: ${result.verificationEvidence}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        assertEquals(0, result.exitCode)
        assertNotNull(result.diff)
        assertTrue(result.stdout.contains("Created isolated workspace"))
        assertTrue(result.stdout.contains("Workspace destroyed: true"))
    }

    @Test
    fun testOpenHandsAdapterFallback() = runBlocking {
        val broker = FederatedWorkerBroker()
        
        val job = PhysicalWorkerJob(
            workerType = ExternalWorkerType.OPEN_HANDS,
            targetRepository = "test-repo",
            instruction = "Implement test integration",
            workspaceIsolated = true
        )
        
        val result = broker.submitJob(job)
        assertEquals(-1, result.exitCode)
        assertTrue(result.verificationEvidence.contains("OpenHands external API failed to respond"))
    }
}
