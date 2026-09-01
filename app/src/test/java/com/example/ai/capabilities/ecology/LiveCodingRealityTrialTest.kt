package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class LiveCodingRealityTrialTest {

    @Test
    fun testFirstBoundedCodingTrial() = runBlocking {
        val broker = FederatedWorkerBroker() // Contains the Native Sandbox fallback adapter
        val orchestrator = CodingRealityTrialOrchestrator(broker)
        
        val auth = CodingTrialAuthorization(
            repositoryId = "local-test-repo",
            repositoryCommitSha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            allowedWorkspace = System.getProperty("java.io.tmpdir") + "/orchestrator_workspace",
            allowedPaths = listOf("src/main/"),
            prohibitedPaths = listOf(".git/", "build/"),
            allowedOperations = setOf("READ", "MODIFY_ALLOWED_PATHS", "RUN_TESTS"),
            networkPolicy = "OFFLINE_ONLY",
            budgetTokens = 500,
            timeoutMs = 10000L,
            rollbackRequirement = true,
            verificationRequirements = listOf("INDEPENDENT_DIFF_HASH", "WORKSPACE_CLEANUP_VERIFICATION")
        )
        
        val instruction = "Add a single diagnostic log behind the debug boundary in AppTest.kt"
        
        val evidence = orchestrator.runTrial(auth, instruction)
        
        println("━━━━━━━━ M. ENGINE — LIVE CODING REALITY TRIAL ━━━━━━━━")
        println("AUTHORIZATION")
        println("BOUNDED_AUTOMATION")
        println("\nOBSERVED")
        println("Repository snapshot acquired.")
        println("Starting commit: ${auth.repositoryCommitSha}")
        println("\nWORKER_REPORTED_RESULT")
        println("Worker reports task completion.")
        println("\nINDEPENDENT VERIFICATION")
        println("Repository modification:\n${if(evidence.preTaskStateObserved && evidence.postTaskStateObserved) "OBSERVED" else "UNKNOWN"}")
        println("\nChanged files:\n${if(evidence.changedFiles.isNotEmpty()) "OBSERVED" else "UNKNOWN"}")
        println("\nDiff hash:\n${evidence.diffHash?.let { "OBSERVED ($it)" } ?: "UNKNOWN"}")
        println("\nBuild:\n${evidence.buildStatus}")
        println("\nTests:\n${evidence.testStatus}")
        println("\nWorkspace cleanup:\n${if(evidence.workspaceCleanupObserved) "OBSERVED" else "UNKNOWN"}")
        println("\nFINAL STATUS")
        println("${evidence.finalStatus} (${evidence.reliabilityState})")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // Assert the evidence proves what was demanded
        assertEquals("VERIFIED_PARTIAL", evidence.finalStatus)
        assertEquals(TrialReliabilityState.FIRST_SUCCESS_OBSERVED, evidence.reliabilityState)
        assertTrue(evidence.workspaceCleanupObserved)
        assertNotNull("Diff hash must not be null (worker must have produced physical evidence)", evidence.diffHash)
        assertEquals("PASS", evidence.buildStatus)
        assertEquals("PASS", evidence.testStatus)
    }
}
