package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The BlendForge worker runs Blender with a Python script and a shell behind it.
 * A capability provider in front of that must be an allowlist, not a passthrough
 * — otherwise the fabric becomes an arbitrary-execution hole with a friendly
 * name. These tests pin that boundary.
 */
class BlendForgeAllowlistTest {

    @Test
    fun `only the three declared operations resolve`() {
        assertEquals(
            listOf("inspect", "normalize", "convert"),
            BlendForgeOperation.entries.map { it.wireName },
        )
        assertNotNull(BlendForgeOperation.fromWire("inspect"))
        assertNotNull(BlendForgeOperation.fromWire("NORMALIZE"))
        assertNotNull(BlendForgeOperation.fromWire("Convert"))
    }

    @Test
    fun `anything not on the allowlist does not resolve`() {
        listOf(
            "exec", "shell", "run", "bash", "python", "eval",
            "../convert", "convert;rm -rf /", "convert && whoami", "",
        ).forEach {
            assertNull("'$it' must not resolve to an operation", BlendForgeOperation.fromWire(it))
        }
    }

    @Test
    fun `a non-allowlisted objective is refused before any dispatch`() = runTest {
        // The client points at a port with nothing on it, so if this test ever
        // reached the network it would fail differently. It must be REFUSED or
        // BLOCKED at the provider, never forwarded.
        val provider = BlendForgeProvider(BlendForgeClient("http://127.0.0.1:1"))
        val result = provider.execute(
            CapabilityAuthorization(governorSessionId = "test"),
            CapabilityTask(
                taskId = "t1",
                objective = "shell",
                contextPayload = "{}",
            ),
        )
        assertEquals(-1, result.exitCode)
        assertNotNull(result.error)
        assertTrue(
            "must be refused or blocked, was: ${result.error}",
            result.error!!.startsWith("REFUSED") || result.error!!.startsWith("BLOCKED"),
        )
    }

    @Test
    fun `an unreachable worker is UNAVAILABLE and says why`() = runTest {
        val probe = BlendForgeProvider(BlendForgeClient("http://127.0.0.1:1")).probe()
        assertEquals(
            com.example.ai.capabilities.federated.environment.FabricNodeState.UNAVAILABLE,
            probe.status,
        )
        assertTrue(
            "the gap must name itself: ${probe.error}",
            probe.error.orEmpty().contains("CAPABILITY_GAP"),
        )
    }

    @Test
    fun `bolt_diy is UNAVAILABLE when no instance runs, however present the source is`() = runTest {
        val probe = BoltDiyProvider(BoltDiyClient("http://127.0.0.1:1")).probe()
        assertEquals(
            com.example.ai.capabilities.federated.environment.FabricNodeState.UNAVAILABLE,
            probe.status,
        )
        assertTrue(
            "must say the repository is not the capability: ${probe.error}",
            probe.error.orEmpty().contains("not the capability"),
        )
    }
}
