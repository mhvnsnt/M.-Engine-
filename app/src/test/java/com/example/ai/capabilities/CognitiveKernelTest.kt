package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CognitiveKernelTest {

    private lateinit var repo: JobStateRepository
    private lateinit var kernel: CognitiveKernelImpl

    @Before
    fun setup() {
        repo = FirebaseJobStateRepositoryMock()
        kernel = CognitiveKernelImpl(repo, "job-123", CognitiveState.QUEUED)
    }

    @Test
    fun testValidTransitions() = runBlocking {
        assertEquals(CognitiveState.QUEUED, kernel.currentState)
        
        kernel.transitionTo(CognitiveState.UNDERSTAND)
        assertEquals(CognitiveState.UNDERSTAND, kernel.currentState)
        assertEquals("UNDERSTAND", repo.getJobState("job-123"))

        kernel.transitionTo(CognitiveState.RESEARCH)
        kernel.transitionTo(CognitiveState.RETRIEVE)
        kernel.transitionTo(CognitiveState.PLAN)
        kernel.transitionTo(CognitiveState.RISK_EVALUATION)
        kernel.transitionTo(CognitiveState.WAITING_APPROVAL)
        
        // Simulating human approval
        if (repo.approveJob("job-123")) {
            // Usually backend signals this, but here we sync
            kernel.transitionTo(CognitiveState.DELEGATE)
        }
        
        kernel.transitionTo(CognitiveState.SANDBOX_CREATING)
        kernel.transitionTo(CognitiveState.REPOSITORY_LOADING)
        kernel.transitionTo(CognitiveState.WORKER_STARTING)
        kernel.transitionTo(CognitiveState.EXECUTING)
        kernel.transitionTo(CognitiveState.BUILDING)
        kernel.transitionTo(CognitiveState.VERIFYING)
        kernel.transitionTo(CognitiveState.COMPLETED)
        
        assertEquals(CognitiveState.COMPLETED, kernel.currentState)
    }

    @Test
    fun testInvalidTransition() = runBlocking {
        kernel.transitionTo(CognitiveState.UNDERSTAND)
        try {
            // Cannot skip from UNDERSTAND straight to COMPLETED
            kernel.transitionTo(CognitiveState.COMPLETED)
            fail("Expected InvalidCognitiveTransitionException")
        } catch (e: InvalidCognitiveTransitionException) {
            // Pass
            assertEquals(CognitiveState.UNDERSTAND, kernel.currentState)
        }
    }

    @Test
    fun testMaxAdaptationLoopFailure() = runBlocking {
        kernel.transitionTo(CognitiveState.UNDERSTAND)
        kernel.transitionTo(CognitiveState.PLAN)
        kernel.transitionTo(CognitiveState.RISK_EVALUATION)
        kernel.transitionTo(CognitiveState.DELEGATE)
        kernel.transitionTo(CognitiveState.SANDBOX_CREATING)
        kernel.transitionTo(CognitiveState.REPOSITORY_LOADING)
        kernel.transitionTo(CognitiveState.WORKER_STARTING)
        
        for (i in 1..5) {
            kernel.transitionTo(CognitiveState.EXECUTING)
            kernel.transitionTo(CognitiveState.VERIFYING)
            kernel.transitionTo(CognitiveState.REFLECTING)
            kernel.transitionTo(CognitiveState.ADAPTING)
        }
        
        // The 6th transition to ADAPTING should force a failure due to max iterations
        kernel.transitionTo(CognitiveState.EXECUTING)
        kernel.transitionTo(CognitiveState.VERIFYING)
        kernel.transitionTo(CognitiveState.REFLECTING)
        kernel.transitionTo(CognitiveState.ADAPTING)
        
        assertEquals(CognitiveState.FAILED, kernel.currentState)
        assertEquals("FAILED", repo.getJobState("job-123"))
    }

    @Test
    fun testCancellation() = runBlocking {
        kernel.transitionTo(CognitiveState.UNDERSTAND)
        kernel.cancelJob()
        assertEquals(CognitiveState.CANCELLED, kernel.currentState)
        assertEquals("CANCELLED", repo.getJobState("job-123"))
        
        // Terminal state should reject further transitions
        try {
            kernel.transitionTo(CognitiveState.UNDERSTAND)
            fail("Expected exception transitioning from cancelled")
        } catch (e: InvalidCognitiveTransitionException) {
            // Pass
        }
    }
}
