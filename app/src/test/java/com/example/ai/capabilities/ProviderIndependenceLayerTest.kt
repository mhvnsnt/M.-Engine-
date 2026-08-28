package com.example.ai.capabilities

import com.example.data.EndpointEntity
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.ConnectException

@RunWith(RobolectricTestRunner::class)
class ProviderIndependenceLayerTest {

    @Test
    fun testErrorClassifier_RateLimit429() {
        val error = ProviderErrorClassifier.classify(null, 429, "Rate limit reached. Please retry in 20s.")
        assertEquals(ProviderErrorKind.RATE_LIMITED, error.kind)
        assertTrue(error.isRetryable)
        assertTrue(error.recommendedCooldownMs > 0)
    }

    @Test
    fun testErrorClassifier_QuotaExhausted402() {
        val error = ProviderErrorClassifier.classify(null, 402, "You have exceeded your current quota or credit balance is too low.")
        assertEquals(ProviderErrorKind.QUOTA_EXHAUSTED, error.kind)
        assertFalse(error.isRetryable)
        assertEquals(15 * 60 * 1000L, error.recommendedCooldownMs)
    }

    @Test
    fun testErrorClassifier_AuthFailed401() {
        val error = ProviderErrorClassifier.classify(null, 401, "Invalid API key provided.")
        assertEquals(ProviderErrorKind.AUTH_FAILED, error.kind)
        assertFalse(error.isRetryable)
    }

    @Test
    fun testErrorClassifier_NetworkConnectionRefused() {
        val error = ProviderErrorClassifier.classify(ConnectException("Connection refused to 127.0.0.1:11434"))
        assertEquals(ProviderErrorKind.NETWORK_ERROR, error.kind)
        assertTrue(error.isRetryable)
    }

    @Test
    fun testMetricsTracker_SuccessAndReliability() {
        val tracker = ProviderMetricsTracker()
        val key = "OPENAI:https://api.openai.com/v1:gpt-4o"

        assertTrue(tracker.isAvailable(key))
        assertEquals(0.85f, tracker.getReliabilityScore(key), 0.01f)

        // Record 5 fast successes
        for (i in 1..5) {
            tracker.recordSuccess(key, "OPENAI", 300L)
        }

        val metrics = tracker.getMetrics(key)
        assertNotNull(metrics)
        assertEquals(5L, metrics?.successfulRequests)
        assertEquals(0L, metrics?.failedRequests)
        assertTrue(tracker.getReliabilityScore(key) >= 0.9f)
    }

    @Test
    fun testMetricsTracker_RateLimitCooldown() {
        val tracker = ProviderMetricsTracker()
        val key = "GEMINI:https://generativelanguage.googleapis.com:gemini-3.5-flash"

        val rateLimitError = ProviderErrorInfo(
            kind = ProviderErrorKind.RATE_LIMITED,
            httpCode = 429,
            rawMessage = "Too Many Requests",
            recommendedCooldownMs = 60000L,
            isRetryable = true
        )

        tracker.recordFailure(key, "GEMINI", rateLimitError)

        assertFalse("Endpoint should be unavailable during cooldown", tracker.isAvailable(key))
        assertEquals(0.0f, tracker.getReliabilityScore(key), 0.001f)

        // Reset cooldown
        tracker.resetCooldown(key)
        assertTrue("Endpoint should be available after reset", tracker.isAvailable(key))
    }

    @Test
    fun testOfflineFallbackProvider() = runBlocking {
        val provider = OfflineFallbackProvider()
        val config = object : EndpointConfig {
            override val url = "local://offline"
            override val apiKey = ""
            override val modelName = "offline-engine"
        }

        val health = provider.healthCheck(config)
        assertEquals(ProviderStatus.ONLINE, health.status)

        val request = ModelRequest(
            systemPrompt = "You are M. Engine core.",
            messages = listOf(ModelMessage(role = "user", content = "check status")),
            endpointConfig = config
        )

        val response = provider.generate(request)
        assertTrue(response.text.contains("M. Engine Offline"))
        assertTrue(response.isFallback)

        val streamChunks = provider.stream(request).toList()
        assertTrue(streamChunks.isNotEmpty())
        assertTrue(streamChunks.any { it.chunk.isNotEmpty() })
    }

    @Test
    fun testModelRouter_FallbackToOfflineWhenNoEndpoints() = runBlocking {
        val registry = CapabilityRegistryImpl().apply {
            register(OfflineFallbackProvider())
        }
        val router = ModelRouter(registry)

        val response = router.generate(
            endpoints = emptyList(),
            systemPrompt = "System instruction",
            messages = listOf(ModelMessage(role = "user", content = "help with git"))
        )

        assertNotNull(response)
        assertTrue(response.isFallback || response.providerUsed == "OfflineFallback")
    }

    @Test
    fun testModelRouter_PrioritizesAvailableEndpoints() {
        val registry = CapabilityRegistryImpl().apply {
            register(OpenRouterProvider())
            register(OllamaProvider())
        }
        val router = ModelRouter(registry)

        val ep1 = EndpointEntity(
            id = 1,
            name = "Failing Endpoint",
            url = "https://failing.api/v1",
            apiKey = "key",
            modelName = "fail-model",
            type = "OPENROUTER",
            isActive = true,
            isPrimary = false
        )
        val ep2 = EndpointEntity(
            id = 2,
            name = "Healthy Endpoint",
            url = "https://healthy.api/v1",
            apiKey = "key",
            modelName = "good-model",
            type = "OPENROUTER",
            isActive = true,
            isPrimary = false
        )

        // Set ep1 into active cooldown
        val key1 = "${ep1.type}:${ep1.url}:${ep1.modelName}"
        router.metricsTracker.setCooldown(key1, ep1.type, 60000L, "Simulated failure")

        val prioritized = router.prioritizeEndpoints(listOf(ep1, ep2), requiresVision = false, requiresTools = false)
        assertEquals("Healthy endpoint should be prioritized before cooling-down endpoint", 2, prioritized.first().id)
    }
}
