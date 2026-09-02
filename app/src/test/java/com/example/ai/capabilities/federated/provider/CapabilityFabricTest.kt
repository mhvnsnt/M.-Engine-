package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

/**
 * Verifies the Capability Fabric performs REAL network probes and reports
 * honestly.
 *
 * This deliberately does not mock the HTTP layer. A mocked probe would prove
 * only that the mock works — the exact substitution REALITY_CONTRACT.md §7
 * rejects. Instead a real HTTP server is bound to a real port, and the fabric is
 * pointed at it, so a passing test means bytes actually crossed a socket.
 */
class CapabilityFabricTest {

    private fun startServer(status: Int): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/health") { exchange ->
                exchange.sendResponseHeaders(status, -1)
                exchange.close()
            }
            start()
        }

    @Test
    fun `probe reports AVAILABLE only when a backend actually answers`() = runTest {
        val server = startServer(200)
        val port = server.address.port
        try {
            // LiteLLM points at the live server; every other endpoint points at a
            // port deliberately chosen to have nothing on it.
            val fabric = CapabilityFabric(
                FabricEndpoints(
                    liteLlm = "http://127.0.0.1:$port",
                    openHands = "http://127.0.0.1:1",
                    hatchet = "http://127.0.0.1:1",
                    playwright = "http://127.0.0.1:1",
                    minio = "http://127.0.0.1:1",
                    postgresHost = "127.0.0.1",
                    postgresPort = 1,
                    // Pinned rather than left at the 8770 default: a test that
                    // depends on nothing happening to listen on the real port
                    // is flaky by construction.
                    unrealWorker = "http://127.0.0.1:1",
                ),
            )

            val catalog = fabric.probeAll()

            // Every registered provider must be reported on. Unreal joined the
            // fabric as the eighth; a provider that is registered but missing
            // from the catalogue is invisible to the owner, which is the whole
            // failure this test exists to catch.
            assertEquals(8, catalog.size)

            // Nothing is listening on the Unreal worker port here, so it must
            // read UNAVAILABLE — never AVAILABLE by virtue of being configured.
            val unreal = catalog.single { it.providerId == "unreal_remote_worker" }
            assertEquals(
                "a worker with nothing listening must not read AVAILABLE",
                FabricNodeState.UNAVAILABLE,
                unreal.status,
            )

            val liteLlm = catalog.single { it.providerId == "litellm_primary" }
            assertEquals(
                "a backend returning 200 on /health must read AVAILABLE",
                FabricNodeState.AVAILABLE,
                liteLlm.status,
            )
            assertEquals("REAL_AND_CONNECTED", liteLlm.realityState)

            val openHands = catalog.single { it.providerId == "openhands_primary" }
            assertEquals(
                "an absent backend must read UNAVAILABLE, never AVAILABLE",
                FabricNodeState.UNAVAILABLE,
                openHands.status,
            )
            assertEquals("BLOCKED_BY_EXTERNAL_DEPENDENCY", openHands.realityState)
            assertNotNull("an unavailable provider must say why", openHands.error)
            assertTrue(
                "the reason must name the capability gap",
                openHands.error!!.contains("CAPABILITY_GAP"),
            )

            // availableProviders must reflect the measurement, not the registry.
            assertEquals(1, fabric.availableProviders(CapabilityType.MODEL_INFERENCE).size)
            assertEquals(0, fabric.availableProviders(CapabilityType.CODING).size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `a provider whose backend disappears stops reporting AVAILABLE`() = runTest {
        val server = startServer(200)
        val port = server.address.port
        val fabric = CapabilityFabric(FabricEndpoints(liteLlm = "http://127.0.0.1:$port"))

        val before = fabric.probeAll().single { it.providerId == "litellm_primary" }
        assertEquals(FabricNodeState.AVAILABLE, before.status)

        // Kill the backend. A cached "AVAILABLE" here would be exactly the stale
        // status the reality contract forbids.
        server.stop(0)

        val after = fabric.probeAll().single { it.providerId == "litellm_primary" }
        assertEquals(
            "availability must be re-measured, never remembered",
            FabricNodeState.UNAVAILABLE,
            after.status,
        )
    }

    @Test
    fun `the in-process native fallback needs no external runtime`() = runTest {
        val fabric = CapabilityFabric(
            FabricEndpoints(
                openHands = "http://127.0.0.1:1",
                hatchet = "http://127.0.0.1:1",
                liteLlm = "http://127.0.0.1:1",
                playwright = "http://127.0.0.1:1",
                minio = "http://127.0.0.1:1",
                postgresPort = 1,
            ),
        )
        val catalog = fabric.probeAll()
        val native = catalog.single { it.providerId == "m_engine_native_sandbox" }
        assertEquals(
            "the native sandbox runs in-process, so it is available with nothing installed",
            FabricNodeState.AVAILABLE,
            native.status,
        )
    }
}
