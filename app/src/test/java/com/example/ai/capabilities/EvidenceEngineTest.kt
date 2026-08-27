package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class EvidenceEngineTest {

    class MockGameActuator : GameActuator {
        override suspend fun launch(config: Map<String, Any>) = true
        override suspend fun input(command: String, params: Map<String, Any>) {}
        override suspend fun wait(ms: Long) {}
        override suspend fun observe() = ScreenObservation(System.currentTimeMillis(), "path/to/img", "tree")
        override suspend fun recordVideo(durationMs: Long) = "path/to/video"
        override suspend fun captureState() = mapOf<String, Any>()
        override suspend fun replay(sessionTrace: VideoSessionTrace) = true
        override suspend fun terminate() {}
    }

    @Test
    fun testEvidenceAssuranceAndMultimodalVerification() = runBlocking {
        val assuranceEngine = EvidenceAssuranceEngineImpl()
        val verificationEngine = MultimodalVerificationEngineImpl()
        val gameActuator = MockGameActuator()

        // 1. Initial claim without evidence
        val claimScenario = "game-npc-stuck-bug-fix"
        assertFalse(assuranceEngine.evaluateClaim(claimScenario))

        // 2. Perform temporal multimodal verification
        val claim = StructuredClaim(
            scenario = claimScenario,
            seed = "184932",
            durationMs = 20000,
            beforeState = "NPC stuck",
            changeCommit = "abc1234",
            afterState = "NPC navigates around obstacle without getting stuck",
            confidence = EvidenceLevel.MODEL_CLAIM
        )
        
        val evidence = verificationEngine.verifyTemporalBehavior(
            claim = claim,
            actuator = gameActuator
        )
        
        // 3. Record evidence
        assuranceEngine.recordEvidence(evidence)
        
        // 4. Verify claim evaluation
        val evaluationResult = assuranceEngine.evaluateClaim(claimScenario)
        assertTrue(evaluationResult) // Should pass because level is TEMPORAL_MULTIMODAL_EVIDENCE (5) >= RUNTIME_EVIDENCE (3)
        
        // 5. Explicitly require a specific level
        assertTrue(assuranceEngine.requireLevel(claimScenario, EvidenceLevel.BEHAVIORAL_EVIDENCE))
        assertFalse(assuranceEngine.requireLevel(claimScenario, EvidenceLevel.INDEPENDENT_VERIFICATION))
    }
}
