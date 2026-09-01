package com.example.ai.capabilities.federated.environment

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class ExecutionEnvironmentTest {

    @Test
    fun testEnvironmentPlacement() = runBlocking {
        val aiStudioEnv = AIStudioSandboxEnvironment()
        aiStudioEnv.probeCapabilities() // Load physical evidence
        
        val remoteWorkerEnv = RemoteNativeWorkerEnvironment()
        remoteWorkerEnv.probeCapabilities()

        val placementEngine = ExecutionPlacementEngine(listOf(aiStudioEnv, remoteWorkerEnv))

        // Requirement 1: Simple shell task (no special requirements)
        val req1 = ExecutionPlacementEngine.PlacementRequirement()
        val result1 = placementEngine.selectEnvironment(aiStudioEnv, req1)
        
        assertEquals(aiStudioEnv.environmentId, result1.selectedEnvironment?.environmentId)
        assertEquals("CURRENT_ENVIRONMENT_SELECTED", result1.status)
        println("Task 1 selected: " + result1.selectedEnvironment?.environmentName)

        // Requirement 2: Needs Docker
        val req2 = ExecutionPlacementEngine.PlacementRequirement(requiresDocker = true)
        val result2 = placementEngine.selectEnvironment(aiStudioEnv, req2)
        
        // Should select remote worker because AI Studio lacks Docker
        assertEquals(remoteWorkerEnv.environmentId, result2.selectedEnvironment?.environmentId)
        assertEquals("AUTHORIZED_ALTERNATIVE_SELECTED", result2.status)
        println("Task 2 selected: " + result2.selectedEnvironment?.environmentName + " (Fallback: " + result2.fallbackReason + ")")

        // Requirement 3: Needs GPU (neither has it)
        val req3 = ExecutionPlacementEngine.PlacementRequirement(requiresGpu = true)
        val result3 = placementEngine.selectEnvironment(aiStudioEnv, req3)
        
        assertNull(result3.selectedEnvironment)
        assertEquals("CAPABILITY_GAP", result3.status)
        println("Task 3 selected: NONE (Reason: " + result3.fallbackReason + ")")
    }
}
