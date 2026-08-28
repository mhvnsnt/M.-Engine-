package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineFallbackRealityContractTest {

    private val provider = OfflineFallbackProvider()

    @Test
    fun testOfflineFallback_AllowsDeterministicStatus() = runBlocking {
        val request = ModelRequest(
            systemPrompt = "",
            messages = listOf(ModelMessage(role = "user", content = "What is the system status and health?")),
            endpointConfig = object : EndpointConfig {
                override val url = "local://offline"
                override val apiKey = ""
                override val modelName = "offline-ast"
                override val providerType = "OFFLINE"
            }
        )

        val response = provider.generate(request)
        assertTrue(response.isFallback)
        assertEquals("stop", response.finishReason)
        assertTrue(response.text.contains("Operational"))
        assertTrue(response.text.contains("Local Room database"))
    }

    @Test
    fun testOfflineFallback_StrictlyRefusesFabricatedCognitiveReasoning() = runBlocking {
        val request = ModelRequest(
            systemPrompt = "You are an AI assistant.",
            messages = listOf(ModelMessage(role = "user", content = "Write a complete Kotlin inventory management system with Spanner sync")),
            endpointConfig = object : EndpointConfig {
                override val url = "local://offline"
                override val apiKey = ""
                override val modelName = "offline-ast"
                override val providerType = "OFFLINE"
            }
        )

        val response = provider.generate(request)
        assertTrue("Must be marked as fallback", response.isFallback)
        assertEquals("blocked_offline_pending_intelligence", response.finishReason)
        assertTrue("Must declare blocked cognitive boundary", response.text.contains("[OFFLINE_BLOCKED_PENDING_INTELLIGENCE]"))
        assertTrue("Must cite Reality Contract", response.text.contains("Reality Contract"))
        assertFalse("Must not claim code was generated", response.text.contains("class InventoryManager"))
    }

    @Test
    fun testOfflineFallback_PreservesMissionCheckpointAndQueuesTasks() {
        val checkpoint = provider.preserveMissionCheckpoint(
            missionId = "miss-test-123",
            stage = "PLAN",
            reason = "Upstream provider rate limit"
        )
        assertNotNull(checkpoint)
        assertEquals("miss-test-123", checkpoint.missionId)
        assertEquals("PLAN", checkpoint.stage)

        val retrieved = provider.getCheckpoint("miss-test-123")
        assertNotNull(retrieved)
        assertEquals("Upstream provider rate limit", retrieved?.reason)

        val queuedTask = provider.queueTask("Synthesize Room entity migration")
        assertEquals(1, provider.getQueuedTasks().size)
        assertEquals("Synthesize Room entity migration", provider.getQueuedTasks().first().prompt)
    }

    @Test
    fun testOfflineFallback_LocalFileInspection() {
        val tempFile = File.createTempFile("test_inspect", ".kt")
        try {
            tempFile.writeText("package com.example.test\n\nclass TestClass {\n    val x = 42\n}\n")
            val inspection = provider.inspectLocalFile(tempFile)
            assertTrue(inspection.contains("lines"))
            assertTrue(inspection.contains("package com.example.test"))
        } finally {
            tempFile.delete()
        }
    }
}
