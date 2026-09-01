package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.*
import com.example.ai.capabilities.federated.environment.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TaskRoutingIntegrationTest {

    class MockJobStateRepository : JobStateRepository {
        private val durableStore = mutableMapOf<String, String>()
        
        override suspend fun updateJobState(jobId: String, status: String, resultMessage: String?): Boolean {
            durableStore[jobId] = status
            return true
        }

        override suspend fun getJobState(jobId: String): String? {
            return durableStore[jobId]
        }

        override suspend fun approveJob(jobId: String): Boolean = true
        override suspend fun rejectJob(jobId: String): Boolean = true
    }

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
    fun testTaskRoutingToPhysicalWorkerThroughCognitiveKernel() = runBlocking {
        // 1. Initialize Kernel and Mock Dependencies
        val repo = MockJobStateRepository()
        val kernel = CognitiveKernelImpl(repo, "job-123", CognitiveState.QUEUED)
        
        val localEnv = MockLocalEnvironment()
        val remoteWorker = RemoteFabricWorkerEnvironment("http://localhost:9092", "test_secret")
        
        // Wait, probeCapabilities must be called to populate actual capabilities from the worker daemon
        val caps = remoteWorker.probeCapabilities()
        println("Probed remote capabilities: $caps")
        
        val placementEngine = ExecutionPlacementEngine(listOf(localEnv, remoteWorker))
        
        // 2. Initialize the Task Router
        val taskRouter = CapabilityTaskRouter(kernel, placementEngine, localEnv)
        
        // 3. Define the Task
        val commandToRun = "echo 'Task Routing Evidence'"
        val task = AutonomousWorkerTask(
            taskId = "task-routing-01",
            goal = "Verify Task Routing over HTTP",
            role = WorkerRole.TERMINAL,
            parameters = mapOf(
                "command" to commandToRun,
                "requiresUnrestrictedShell" to "true"
            ),
            context = ""
        )
        
        // 4. Execute Router Process
        val result = taskRouter.routeAndExecute(task)
        
        // 5. Assertions on evidence and kernel state
        assertTrue("Execution should be successful. Err: ${result.errorMessage}", result.isSuccess)
        assertEquals("Python Fabric Worker", result.artifacts["environment"])
        assertTrue("Output should contain the evidence string", result.output.contains("Task Routing Evidence"))
        
        assertEquals(CognitiveState.COMPLETED, kernel.currentState)
        assertEquals(CognitiveState.COMPLETED.name, repo.getJobState("job-123"))
        
        println("Task routed and executed successfully!")
        println("Final Output: ${result.output}")
        println("Workspace Lifecycle ID: ${result.artifacts["workspace"]}")
    }
}
