package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

/**
 * Verifies the Unreal provider distinguishes the three states that matter and
 * never manufactures availability.
 *
 * Not mocked at the transport: a real HTTP server on a real port serves the
 * exact JSON shape the Node worker in tools/unreal-worker emits, so a pass means
 * the client genuinely parsed an HTTP response. The response bodies here were
 * copied from an actual run of that worker against the real Bannon repository,
 * not invented.
 */
// Robolectric supplies the real org.json; the plain JVM unit-test android.jar
// stubs it and every method throws "not mocked".
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class UnrealWorkerProviderTest {

    private fun serve(capabilitiesJson: String): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/health") { ex ->
                val b = """{"status":"UP","worker":"unreal","version":1}""".toByteArray()
                ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
            }
            createContext("/capabilities") { ex ->
                val b = capabilitiesJson.toByteArray()
                ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
            }
            start()
        }

    /** Verbatim shape from a real worker run on a host with no engine. */
    private val noEngine = """
    {"probedAt":"2026-09-01T20:00:00Z",
     "host":{"platform":"linux","arch":"x64","hostname":"builder","cpus":4,"totalMemGb":17},
     "capabilities":{
       "UNREAL_RUNTIME_DISCOVERED":{"state":"CAPABILITY_GAP","engineRoot":null,"version":null,
         "evidence":"no UnrealEditor-Cmd binary found"},
       "UNREAL_BUILD_CAPABLE":{"state":"CAPABILITY_GAP","evidence":"no engine root; build tool cannot exist"},
       "UNREAL_PROJECT_AVAILABLE":{"state":"VERIFIED","evidence":"1 .uproject file(s) found"},
       "ANDROID_TOOLCHAIN_AVAILABLE":{"state":"CAPABILITY_GAP","evidence":"missing: Android SDK"},
       "PHYSICAL_DEVICE_AVAILABLE":{"state":"CAPABILITY_GAP","evidence":"adb not available"}}}
    """.trimIndent()

    private val withEngine = """
    {"probedAt":"2026-09-01T20:00:00Z",
     "host":{"platform":"win32","arch":"x64","hostname":"studio-pc","cpus":16,"totalMemGb":64},
     "capabilities":{
       "UNREAL_RUNTIME_DISCOVERED":{"state":"VERIFIED","engineRoot":"C:\\Program Files\\Epic Games\\UE_5.3",
         "version":"5.3.2","evidence":"executed UnrealEditor-Cmd.exe -Version, exit 0"},
       "UNREAL_BUILD_CAPABLE":{"state":"PARTIALLY_VERIFIED","evidence":"build script present"},
       "UNREAL_PROJECT_AVAILABLE":{"state":"VERIFIED","evidence":"1 .uproject file(s) found"},
       "ANDROID_TOOLCHAIN_AVAILABLE":{"state":"PARTIALLY_VERIFIED","evidence":"SDK, NDK and JDK all present"},
       "PHYSICAL_DEVICE_AVAILABLE":{"state":"VERIFIED","evidence":"1 device(s) reported by adb"}}}
    """.trimIndent()

    @Test
    fun `no worker at all is UNAVAILABLE with an actionable reason`() = runTest {
        // Port 1 has nothing on it.
        val provider = UnrealExecutionProvider(UnrealWorkerClient("http://127.0.0.1:1"))
        val result = provider.probe()
        assertEquals(FabricNodeState.UNAVAILABLE, result.status)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("CAPABILITY_GAP"))
        // The message must tell the owner what to actually do.
        assertTrue(result.error!!.contains("tools/unreal-worker"))
    }

    @Test
    fun `a reachable worker WITHOUT an engine is never AVAILABLE`() = runTest {
        val server = serve(noEngine)
        try {
            val provider = UnrealExecutionProvider(
                UnrealWorkerClient("http://127.0.0.1:${server.address.port}"),
            )
            val result = provider.probe()

            // This is the distinction that keeps a green light meaningful:
            // "the worker is up" is not "Unreal can build".
            assertEquals(
                "a worker with no engine must NOT read AVAILABLE",
                FabricNodeState.PARTIALLY_VERIFIED,
                result.status,
            )
            assertTrue(result.error!!.contains("no Unreal Engine on that host"))
            assertEquals("builder", result.details["host"])
            assertEquals("CAPABILITY_GAP", result.details["ANDROID_TOOLCHAIN_AVAILABLE"])
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `a worker with a verified engine is AVAILABLE and reports its version`() = runTest {
        val server = serve(withEngine)
        try {
            val provider = UnrealExecutionProvider(
                UnrealWorkerClient("http://127.0.0.1:${server.address.port}"),
            )
            val result = provider.probe()

            assertEquals(FabricNodeState.AVAILABLE, result.status)
            assertEquals("5.3.2", result.details["engineVersion"])
            assertEquals("studio-pc", result.details["host"])
            assertEquals("VERIFIED", result.details["PHYSICAL_DEVICE_AVAILABLE"])
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `execute refuses when the engine is not available`() = runTest {
        val server = serve(noEngine)
        try {
            val provider = UnrealExecutionProvider(
                UnrealWorkerClient("http://127.0.0.1:${server.address.port}"),
            )
            val result = provider.execute(
                CapabilityAuthorization(governorSessionId = "s1"),
                CapabilityTask(taskId = "t1", objective = "build Bannon", contextPayload = "/x/Bannon.uproject"),
            )
            assertEquals(-1, result.exitCode)
            assertTrue(result.error!!.contains("BLOCKED"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `the fabric registers Unreal as a capability type`() = runTest {
        val fabric = CapabilityFabric(FabricEndpoints(unrealWorker = "http://127.0.0.1:1"))
        val providers = fabric.registry.getProvidersByType(CapabilityType.GAME_ENGINE_BUILD)
        assertEquals(1, providers.size)
        assertEquals("unreal_remote_worker", providers.first().providerId)
    }
}
