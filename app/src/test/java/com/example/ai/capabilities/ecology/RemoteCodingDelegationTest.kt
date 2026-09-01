package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.AutonomousWorkerTask
import com.example.ai.capabilities.WorkerRole
import com.example.ai.capabilities.federated.environment.ExecutionEnvironment
import com.example.ai.capabilities.federated.environment.ExecutionPlacementEngine
import com.example.ai.capabilities.federated.environment.RemoteFabricCodingWorker
import com.example.ai.capabilities.federated.environment.RemoteFabricWorkerEnvironment
import com.example.ai.capabilities.federated.environment.EnvironmentCapabilities
import com.example.ai.capabilities.federated.environment.CapabilityLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RemoteCodingDelegationTest {

    class MockLocalEnvironment : ExecutionEnvironment {
        override val environmentId = "local-sandbox"
        override val environmentName = "Local App Sandbox"
        
        override val capabilities = EnvironmentCapabilities(
            shellExecution = CapabilityLevel.PARTIAL,
            filesystemRead = CapabilityLevel.PARTIAL,
            filesystemWrite = CapabilityLevel.PARTIAL,
            processSpawning = CapabilityLevel.UNAVAILABLE,
            persistentProcessSupport = CapabilityLevel.UNAVAILABLE,
            networkEgress = CapabilityLevel.UNAVAILABLE,
            inboundNetworkSupport = CapabilityLevel.UNAVAILABLE,
            dockerCli = CapabilityLevel.UNAVAILABLE,
            dockerDaemon = CapabilityLevel.UNAVAILABLE,
            podman = CapabilityLevel.UNAVAILABLE,
            browserAutomation = CapabilityLevel.UNAVAILABLE,
            gpuAvailability = CapabilityLevel.UNAVAILABLE,
            localModelRuntime = CapabilityLevel.UNAVAILABLE,
            databaseAccess = CapabilityLevel.UNAVAILABLE,
            secretAccess = CapabilityLevel.UNAVAILABLE,
            maximumExecutionDurationMs = null,
            persistenceAcrossProcessDeath = CapabilityLevel.UNAVAILABLE
        )
        
        override suspend fun probeCapabilities(): EnvironmentCapabilities = capabilities
    }

    @Test
    fun testDelegatedCodingTaskToPhysicalWorker() = runBlocking {
        val localEnv = MockLocalEnvironment()
        
        val remoteWorker = RemoteFabricWorkerEnvironment("http://localhost:9092", "test_secret")
        val caps = remoteWorker.probeCapabilities()
        
        val placementEngine = ExecutionPlacementEngine(listOf(localEnv, remoteWorker))
        
        val codingWorker = RemoteFabricCodingWorker(placementEngine, localEnv)
        
        val pythonCode = """
            |def bubble_sort(arr):
            |    n = len(arr)
            |    for i in range(n):
            |        for j in range(0, n-i-1):
            |            if arr[j] > arr[j+1]:
            |                arr[j], arr[j+1] = arr[j+1], arr[j]
            |    return arr
            |print(bubble_sort([64, 34, 25, 12, 22, 11, 90]))
        """.trimMargin()
        
        val shellCommand = "cat << 'INNER_EOF' > sort.py\n$pythonCode\nINNER_EOF\npython3 sort.py"
        
        val task = AutonomousWorkerTask(
            taskId = "task-coding-1",
            goal = "Implement and test bubble sort in Python",
            role = WorkerRole.CODER,
            parameters = mapOf(
                "command" to shellCommand
            ),
            context = ""
        )
        
        val result = codingWorker.executeTask(task)
        
        assertTrue("Failed: " + result.output + " | err: " + result.errorMessage, result.isSuccess)
        assertEquals("Python Fabric Worker", result.artifacts["environment"])
        assertTrue(result.output.contains("[11, 12, 22, 25, 34, 64, 90]"))
        
        println("Delegation success. Worker Output: \${result.output}")
        val workspace = result.artifacts["workspace"]
        println("Executed in workspace: \$workspace")
    }
}
