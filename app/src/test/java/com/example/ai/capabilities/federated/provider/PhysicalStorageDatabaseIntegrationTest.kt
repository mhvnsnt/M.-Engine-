package com.example.ai.capabilities.federated.provider

import com.example.ai.capabilities.federated.environment.FabricNodeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalStorageDatabaseIntegrationTest {

    @Test
    fun `test physical Postgres connectivity according to Reality Contract`() = runBlocking {
        val client = PostgresClient("localhost", 5432)
        val provider = PostgresDatabaseProvider(client)

        val probeResult = provider.probe()
        
        println("EVIDENCE: Postgres Probe Status -> \${probeResult.status}")
        if (probeResult.error != null) {
            println("EVIDENCE: Postgres Probe Error -> \${probeResult.error}")
        }
        
        // We expect UNAVAILABLE because Postgres is not physically running on 5432 in this Sandbox
        assertEquals(FabricNodeState.UNAVAILABLE, probeResult.status)
        assertTrue(probeResult.error!!.contains("CAPABILITY_GAP"))
    }
    
    @Test
    fun `test physical MinIO connectivity according to Reality Contract`() = runBlocking {
        val client = MinIOClient("http://localhost:9000")
        val provider = MinIOStorageProvider(client)

        val probeResult = provider.probe()
        
        println("EVIDENCE: MinIO Probe Status -> \${probeResult.status}")
        if (probeResult.error != null) {
            println("EVIDENCE: MinIO Probe Error -> \${probeResult.error}")
        }
        
        // We expect UNAVAILABLE because MinIO is not physically running on 9000 in this Sandbox
        assertEquals(FabricNodeState.UNAVAILABLE, probeResult.status)
        assertTrue(probeResult.error!!.contains("CAPABILITY_GAP"))
    }
}
