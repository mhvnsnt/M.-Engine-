package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FederatedCapabilityFabricTest {

    @Test
    fun testAllCapabilitiesRegisteredInRegistry() {
        val registry = FederatedCapabilityRegistry
        val all = registry.getAllCapabilities()
        assertTrue("Expected at least 9 registered capabilities, got ${all.size}", all.size >= 9)

        val names = all.map { it.name }.toSet()
        val expected = setOf(
            "GitHubWorkerCapability",
            "WebResearchCapability",
            "DocumentationCapability",
            "SandboxExecutionCapability",
            "VideoResearchCapability",
            "DatabaseCapability",
            "LocalModelCapability",
            "RemoteModelCapability",
            "CodingWorkerCapability"
        )
        for (exp in expected) {
            assertTrue("Expected registry to contain $exp", names.contains(exp))
            assertNotNull("Capability lookup for $exp should not be null", registry.getCapability(exp))
        }
    }

    @Test
    fun testCapabilityRealityStatesAndVerification() = runBlocking {
        val cap = GitHubWorkerCapability()
        assertEquals(CapabilityState.IMPLEMENTED_UNVERIFIED, cap.state)
        assertFalse("Unverified capability must not be available yet", cap.isAvailable())

        val health = cap.verifyHealth()
        assertTrue(health.success)
        assertEquals(CapabilityState.AVAILABLE, cap.state)
        assertTrue(cap.isAvailable())
        assertTrue(cap.verificationEvidence.isNotEmpty())
        assertNotNull(cap.lastHealthCheck)
    }

    @Test
    fun testGitHubWorkerCapabilityExecution() = runBlocking {
        val cap = GitHubWorkerCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("repository" to "test/repo", "branch" to "feat/physics"))
        
        assertTrue(result.success)
        assertTrue(result.observations.isNotEmpty())
        assertTrue(result.evidence.isNotEmpty())
        assertTrue(result.artifactsGenerated.isNotEmpty())
        assertTrue(result.executionTimeMs > 0)
        assertNotNull(result.authorizationUsed)
        assertEquals(2, result.costMetrics.networkCalls)
    }

    @Test
    fun testWebResearchCapabilityExecution() = runBlocking {
        val cap = WebResearchCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("query" to "root motion blending in game physics"))
        
        assertTrue(result.success)
        assertTrue(result.observations.any { it.contains("Dual-Queue") || it.contains("Input Buffer") })
        assertTrue(result.evidence.isNotEmpty())
        assertTrue(result.costMetrics.costUsd > 0.0)
    }

    @Test
    fun testDocumentationCapabilityExecution() = runBlocking {
        val cap = DocumentationCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("topic" to "M. Engine Core Architecture"))
        
        assertTrue(result.success)
        assertTrue(result.observations.isNotEmpty())
        assertEquals(0, result.costMetrics.networkCalls)
    }

    @Test
    fun testVideoResearchCapabilityExecution() = runBlocking {
        val cap = VideoResearchCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("videoUri" to "https://internal.test/takedown.mp4"))
        
        assertTrue(result.success)
        assertTrue(result.observations.any { it.contains("stutter") || it.contains("transition") })
        assertTrue(result.evidence.any { it.contains("Frame #") })
    }

    @Test
    fun testDatabaseCapabilityExecution() = runBlocking {
        val cap = DatabaseCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("queryType" to "GET_UNRESOLVED_CONTRADICTIONS"))
        
        assertTrue(result.success)
        assertTrue(result.evidence.any { it.contains("PostgreSQL") })
    }

    @Test
    fun testLocalModelCapabilityZeroCost() = runBlocking {
        val cap = LocalModelCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("task" to "DEDUPLICATION"))
        
        assertTrue(result.success)
        assertEquals(0.0, result.costMetrics.costUsd, 0.0001)
        assertEquals(0, result.costMetrics.networkCalls)
        assertTrue(result.observations.any { it.contains("Ollama") })
    }

    @Test
    fun testRemoteModelCapabilityDeepReasoning() = runBlocking {
        val cap = RemoteModelCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("prompt" to "Architect continuous grappling engine"))
        
        assertTrue(result.success)
        assertTrue(result.costMetrics.costUsd > 0.0)
        assertEquals(1, result.costMetrics.modelCalls)
    }

    @Test
    fun testCodingWorkerCapabilityPatchGeneration() = runBlocking {
        val cap = CodingWorkerCapability()
        cap.verifyHealth()
        assertTrue(cap.isAvailable())
        val result = cap.execute(mapOf("targetFile" to "TransitionController.kt"))
        
        assertTrue(result.success)
        assertTrue(result.artifactsGenerated.any { it.contains(".diff") || it.contains(".patch") })
        assertTrue(result.nextPossibilities.any { it.contains("SandboxExecutionCapability") })
    }

    @Test
    fun testOpportunityEngineRankingFormula() {
        val allCaps = FederatedCapabilityRegistry.getAllCapabilities()
        val rankings = CapabilityRealitySweepEngine.computeRankings(allCaps)
        
        assertEquals(allCaps.size, rankings.size)
        // Verify rankings are strictly sorted in descending score order
        for (i in 0 until rankings.size - 1) {
            assertTrue(
                "Rank ${rankings[i].rank} score (${rankings[i].score.score}) must be >= Rank ${rankings[i+1].rank} score (${rankings[i+1].score.score})",
                rankings[i].score.score >= rankings[i+1].score.score
            )
            assertEquals(i + 1, rankings[i].rank)
        }
        assertEquals(rankings.size, rankings.last().rank)
    }

    @Test
    fun testCapabilityRealitySweepExecutionAndTransitions() = runBlocking {
        FederatedCapabilityRegistry.reset()
        val report = CapabilityRealitySweepEngine.executeSweep()

        assertNotNull(report)
        assertTrue(report.sweepId.startsWith("sweep-"))
        assertEquals(9, report.totalCapabilities)
        assertTrue("Verified operational count must be > 0", report.verifiedOperationalCount > 0)
        assertTrue(report.rankings.isNotEmpty())
        assertTrue(report.transitionsExecuted.isNotEmpty())
        assertTrue(report.summary.contains("Reality Sweep"))

        // Check runtime states have rank assigned
        val runtimeStates = FederatedCapabilityRegistry.getRuntimeStates()
        assertEquals(9, runtimeStates.size)
        assertTrue(runtimeStates.all { it.rank > 0 })
        assertEquals(1, runtimeStates.first().rank)

        // Check transition history recorded on capabilities
        val ghCap = FederatedCapabilityRegistry.getCapability("GitHubWorkerCapability")
        assertNotNull(ghCap)
        assertTrue(ghCap!!.transitionHistory.isNotEmpty())
        val lastTrans = ghCap.transitionHistory.last()
        assertEquals(CapabilityState.AVAILABLE, lastTrans.toState)
        assertTrue(lastTrans.evidence.isNotEmpty())
    }
}
