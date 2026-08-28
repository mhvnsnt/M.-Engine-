package com.example.ai.capabilities

import com.example.data.EndpointEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkloadRoutingAndStatusTest {

    private val registry = CapabilityRegistryImpl().apply {
        register(GeminiProvider())
        register(AnthropicDirectProvider())
        register(OpenRouterProvider())
        register(OllamaProvider())
        register(OfflineFallbackProvider())
    }

    private val router = ModelRouter(registry)

    @Test
    fun testWorkloadBenchmarkMatrix_ScoreLookupAndAdaptation() {
        val matrix = router.workloadMatrix
        val codingScoreClaude = matrix.getScore(WorkloadType.CODING, "ANTHROPIC", "claude-3-5-sonnet")
        val longContextGemini = matrix.getScore(WorkloadType.LONG_CONTEXT, "GEMINI", "gemini-3.5-flash")

        assertTrue("Claude coding score should be top tier", codingScoreClaude >= 0.95f)
        assertTrue("Gemini long context score should be top tier", longContextGemini >= 0.95f)

        // Record empirical failure and verify score degradation
        matrix.recordEmpiricalResult(WorkloadType.CODING, "ANTHROPIC", "claude-3-5-sonnet", false, 1200L)
        val updatedScore = matrix.getScore(WorkloadType.CODING, "ANTHROPIC", "claude-3-5-sonnet")
        assertTrue("Score should adjust with empirical results", updatedScore < codingScoreClaude)
    }

    @Test
    fun testModelRouter_SelectsSpecializedWorkerForWorkload() {
        val claudeEndpoint = EndpointEntity(
            id = 1,
            name = "Claude Direct",
            type = "ANTHROPIC",
            url = "https://api.anthropic.com",
            apiKey = "test-key",
            modelName = "claude-3-5-sonnet",
            isPrimary = false,
            isActive = true
        )
        val geminiEndpoint = EndpointEntity(
            id = 2,
            name = "Gemini Flash",
            type = "GEMINI",
            url = "https://generativelanguage.googleapis.com",
            apiKey = "test-key",
            modelName = "gemini-3.5-flash",
            isPrimary = false,
            isActive = true
        )

        val endpoints = listOf(geminiEndpoint, claudeEndpoint)

        // For CODING workload, Claude should be selected based on domain benchmark
        val codingWorker = router.selectWorkerForWorkload(WorkloadType.CODING, endpoints)
        assertEquals("ANTHROPIC", codingWorker.primaryEndpoint?.type)

        // For LONG_CONTEXT workload, Gemini should be selected
        val contextWorker = router.selectWorkerForWorkload(WorkloadType.LONG_CONTEXT, endpoints)
        assertEquals("GEMINI", contextWorker.primaryEndpoint?.type)
    }

    @Test
    fun testModelRouter_GeneratesFormattedIntelligenceStatusReport() = runBlocking {
        val claudeEndpoint = EndpointEntity(
            id = 1,
            name = "Claude Direct",
            type = "ANTHROPIC",
            url = "https://api.anthropic.com",
            apiKey = "test-key",
            modelName = "claude-3-5-sonnet",
            isPrimary = true,
            isActive = true
        )

        val report = router.getIntelligenceStatusReport(
            endpoints = listOf(claudeEndpoint),
            currentMissionName = "Self-Improvement #2",
            activeWorkload = WorkloadType.CODING
        )

        assertNotNull(report)
        assertEquals("Self-Improvement #2", report.currentMission)
        assertTrue(report.providerEntries.isNotEmpty())

        val formatted = router.formatIntelligenceStatus(report)
        assertTrue(formatted.contains("INTELLIGENCE CONTROL PLANE"))
        assertTrue(formatted.contains("Claude Direct"))
        assertTrue(formatted.contains("Offline Deterministic Engine"))
        assertTrue(formatted.contains("Fallback Chain"))
    }
}
