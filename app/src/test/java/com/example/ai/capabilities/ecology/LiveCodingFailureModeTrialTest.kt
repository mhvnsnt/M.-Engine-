package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class LiveCodingFailureModeTrialTest {

    @Test
    fun testTimeoutFailureMode() = runBlocking {
        val broker = FederatedWorkerBroker()
        val orchestrator = CodingRealityTrialOrchestrator(broker)
        
        val auth = CodingTrialAuthorization(
            repositoryId = "local-test-repo",
            repositoryCommitSha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            allowedWorkspace = System.getProperty("java.io.tmpdir") + "/orchestrator_workspace_timeout",
            allowedPaths = listOf("src/main/"),
            prohibitedPaths = listOf(".git/", "build/"),
            allowedOperations = setOf("READ", "MODIFY_ALLOWED_PATHS", "RUN_TESTS"),
            networkPolicy = "OFFLINE_ONLY",
            budgetTokens = 500,
            timeoutMs = 1L, // Impossible timeout to force failure
            rollbackRequirement = true,
            verificationRequirements = listOf("INDEPENDENT_DIFF_HASH", "WORKSPACE_CLEANUP_VERIFICATION")
        )
        
        // Instruction is irrelevant since it will timeout immediately
        val instruction = "This will timeout."
        
        val evidence = orchestrator.runTrial(auth, instruction)
        
        // Assert the orchestrator catches the failure and still cleans up
        assertEquals("FAILED", evidence.finalStatus)
        assertEquals(TrialReliabilityState.UNVERIFIED, evidence.reliabilityState)
        assertTrue("Workspace must be cleaned up even on failure", evidence.workspaceCleanupObserved)
        assertEquals("UNKNOWN", evidence.buildStatus)
        
        println("━━━━━━━━ M. ENGINE — FAILURE MODE TRIAL (TIMEOUT) ━━━━━━━━")
        println("FINAL STATUS: ${evidence.finalStatus}")
        println("CLEANUP OBSERVED: ${evidence.workspaceCleanupObserved}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    @Test
    fun testMalformedPatchFailureMode() = runBlocking {
        val broker = FederatedWorkerBroker()
        val orchestrator = CodingRealityTrialOrchestrator(broker)
        
        val auth = CodingTrialAuthorization(
            repositoryId = "local-test-repo",
            repositoryCommitSha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            allowedWorkspace = System.getProperty("java.io.tmpdir") + "/orchestrator_workspace_malformed",
            allowedPaths = listOf("src/main/"),
            prohibitedPaths = listOf(".git/", "build/"),
            allowedOperations = setOf("READ", "MODIFY_ALLOWED_PATHS", "RUN_TESTS"),
            networkPolicy = "OFFLINE_ONLY",
            budgetTokens = 500,
            timeoutMs = 10000L,
            rollbackRequirement = true,
            verificationRequirements = listOf("INDEPENDENT_DIFF_HASH", "WORKSPACE_CLEANUP_VERIFICATION")
        )
        
        // This is a special instruction that tells the mock broker to intentionally fail the compilation step
        val instruction = "INTENTIONAL_COMPILATION_FAILURE"
        
        val evidence = orchestrator.runTrial(auth, instruction)
        
        // Assert the orchestrator catches the build failure
        assertEquals("FAILED", evidence.finalStatus)
        assertEquals(TrialReliabilityState.UNVERIFIED, evidence.reliabilityState)
        assertTrue("Workspace must be cleaned up even on failure", evidence.workspaceCleanupObserved)
        
        // Important: diff hash might exist (a patch was made), but build failed
        assertNotNull(evidence.diffHash)
        assertEquals("UNKNOWN", evidence.buildStatus) // Mock broker returns non-zero exit code
        
        println("━━━━━━━━ M. ENGINE — FAILURE MODE TRIAL (MALFORMED PATCH) ━━━━━━━━")
        println("FINAL STATUS: ${evidence.finalStatus}")
        println("DIFF HASH: ${evidence.diffHash}")
        println("BUILD STATUS: ${evidence.buildStatus}")
        println("CLEANUP OBSERVED: ${evidence.workspaceCleanupObserved}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
