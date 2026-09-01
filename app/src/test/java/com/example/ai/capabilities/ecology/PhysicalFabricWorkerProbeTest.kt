package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.federated.environment.CapabilityLevel
import com.example.ai.capabilities.federated.environment.FabricNodeState
import com.example.ai.capabilities.federated.environment.RemoteFabricWorkerEnvironment
import com.example.ai.capabilities.federated.environment.GovernorRegistryServer
import com.example.ai.capabilities.federated.environment.GlobalWorkerRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhysicalFabricWorkerProbeTest {

    @Test
    fun testPhysicalWorkerProbeAndExecution() = runBlocking {
        GlobalWorkerRegistry.instance.clear()
        

        
        // Spawn Python daemon
        val pb = ProcessBuilder("python3", "src/main/python/native_worker.py", "--secret", "test_secret")
        pb.redirectErrorStream(true)
        val process = pb.start()
        
        // Print process output in a background coroutine
        launch(kotlinx.coroutines.Dispatchers.IO) {
            process.inputStream.bufferedReader().use { it.lines().forEach { line -> println("WORKER: $line") } }
        }
        
        // Give the Python daemon time to start
        delay(2000)
        
        // Connect to the real local Python daemon running on 9092
        val worker = RemoteFabricWorkerEnvironment("http://localhost:9092", "test_secret")
        
        // 1. Probe the physical capabilities over HTTP
        val caps = worker.probeCapabilities()
        
        assertEquals(FabricNodeState.AVAILABLE, worker.nodeState)
        assertEquals("Python Physical Worker (Secure)", worker.environmentName)
        assertEquals(CapabilityLevel.VERIFIED, caps.shellExecution)
        assertEquals(CapabilityLevel.VERIFIED, caps.filesystemRead)
        
        // Docker should be genuinely UNAVAILABLE in this specific sandbox environment
        assertEquals(CapabilityLevel.UNAVAILABLE, caps.dockerCli)

        // 2. Perform a physical bounded execution on the worker
        val result = worker.executeCommand("echo 'REALITY_CONTRACT_VERIFIED'")
        
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("REALITY_CONTRACT_VERIFIED"))
        assertTrue(result.stderr.isEmpty())
        
        println("Physical Execution Fabric Worker successfully probed and executed via JOB Protocol.")
        println("Capabilities: $caps")
        

        process.destroy()
    }
}
