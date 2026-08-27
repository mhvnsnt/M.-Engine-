package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration

class Phase5WorkerTest {

    class MockSandboxManager : RemoteSandboxManager {
        val activeSandboxes = mutableListOf<String>()
        val executedCommands = mutableListOf<String>()
        var simulateFailure = false

        override suspend fun provisionSandbox(jobId: String, config: SandboxConfig): String {
            val id = "sandbox-\$jobId"
            activeSandboxes.add(id)
            return id
        }

        override suspend fun destroySandbox(sandboxId: String): Boolean {
            return activeSandboxes.remove(sandboxId)
        }

        override suspend fun cloneRepository(sandboxId: String, repo: RepositoryRef, secureToken: String): Boolean {
            return !simulateFailure
        }

        override suspend fun executeCommand(sandboxId: String, command: String, timeoutMinutes: Int): ExecutionResult {
            executedCommands.add(command)
            if (simulateFailure) {
                return ExecutionResult(1, "", "Simulated failure", false)
            }
            return ExecutionResult(0, "Success", "", false)
        }
    }

    @Test
    fun testWorkerCancellationTerminatesSandbox() = runBlocking {
        val sandboxManager = MockSandboxManager()
        val sandboxId = sandboxManager.provisionSandbox("job-123", SandboxConfig(SandboxLimits(1024, 1.0f, 15), NetworkPolicy.ISOLATED, "ubuntu"))
        val worker = AiderRuntime(sandboxManager, sandboxId)

        assertTrue(sandboxManager.activeSandboxes.contains(sandboxId))
        
        worker.cancel() // Simulated cancel

        assertFalse(sandboxManager.activeSandboxes.contains(sandboxId))
    }

    @Test
    fun testIndependentVerifierChecks() = runBlocking {
        val sandboxManager = MockSandboxManager()
        val sandboxId = sandboxManager.provisionSandbox("job-456", SandboxConfig(SandboxLimits(1024, 1.0f, 15), NetworkPolicy.ISOLATED, "ubuntu"))
        val verifier = IndependentVerifier(sandboxManager, sandboxId)
        
        val evidence = verifier.verifyWorkerOutput()
        
        assertTrue(evidence.buildPass)
        assertTrue(evidence.unitTestsPass)
        assertTrue(evidence.staticAnalysisPass)
        assertTrue(evidence.securityChecksPass)
        assertTrue(evidence.requestedBehaviorVerified)
        
        // Ensure commands were actually sent to the sandbox
        assertTrue(sandboxManager.executedCommands.contains("./gradlew assembleDebug"))
        assertTrue(sandboxManager.executedCommands.contains("./gradlew testDebugUnitTest"))
    }
}
