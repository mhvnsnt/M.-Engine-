package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PhysicalHatchetIntegrationTest {

    @Test
    fun `test physical Hatchet connectivity and dispatch according to Reality Contract`() = runBlocking {
        val client = HatchetClient("http://localhost:8080")
        val provider = HatchetWorkflowProvider(client)

        // 1. Probe the provider
        val probeResult = provider.probe()
        
        // Output for the execution observatory
        println("EVIDENCE: Hatchet Probe Status -> \${probeResult.status}")
        if (probeResult.error != null) {
            println("EVIDENCE: Hatchet Probe Error -> \${probeResult.error}")
        }
        
        // Based on the environment, we know Postgres/RabbitMQ aren't running here
        // so we expect a CAPABILITY_GAP (UNAVAILABLE)
        assertEquals("Hatchet should be UNAVAILABLE due to missing local backend", FabricNodeState.UNAVAILABLE, probeResult.status)
        assertTrue(probeResult.error!!.contains("CAPABILITY_GAP"))

        // 2. Try to dispatch a workflow anyway to verify bounded rejection
        val task = CapabilityTask(
            taskId = UUID.randomUUID().toString(),
            objective = "Test Hatchet Dispatch",
            contextPayload = "{}"
        )
        val auth = CapabilityAuthorization("test-session")
        
        val executionResult = provider.execute(auth, task)
        
        println("EVIDENCE: Hatchet Execution Error -> \${executionResult.error}")
        
        assertEquals(-1, executionResult.exitCode)
        assertTrue(executionResult.error!!.contains("BLOCKED"))
    }
}
