package com.example.ai.capabilities.ecology

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import org.junit.Ignore

@RunWith(RobolectricTestRunner::class)
class LiveDatabaseProbeTest {
    
    private lateinit var db: AppDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        FederatedCapabilityRegistry.reset()
    }
    
    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testLiveDatabaseCapabilityProbe() = runBlocking {
        // Inject the real DB instance into the environment for the probe to use
        val dbCap = FederatedCapabilityRegistry.getCapability("DatabaseCapability") as DatabaseCapability
        dbCap.injectRealDatabase(db)
        
        println("━━━━━━━━ M. ENGINE — LIVE CAPABILITY VERIFICATION ━━━━━━━━\n")
        println("CAPABILITY\n${dbCap.name}\n")
        
        val result = dbCap.verifyHealth()
        
        val sqliteStatus = result.evidence.find { it.startsWith("SQLite Execution:") }?.substringAfter(": ") ?: "N/A"
        val latency = result.evidence.find { it.startsWith("LATENCY:") }?.substringAfter(": ")?.replace("ms", "") ?: "N/A"
        val dbWriteVerified = result.evidence.find { it.startsWith("Write Verification:") }?.substringAfter(": ") ?: "UNKNOWN"
        val stateVerified = result.evidence.find { it.startsWith("State Persistence:") } != null
        
        println("OBSERVED")
        println(if (result.success) "Live Room Database connection verified." else "Probe failed: ${result.failureReason}")
        println("\nDATABASE STATUS\n$sqliteStatus")
        println("\nLATENCY\n$latency ms")
        println("\nWRITE VERIFICATION\n$dbWriteVerified")
        
        if (stateVerified) {
            println("\nEVIDENCE\nDatabase capabilities confirmed operational via test read/write cycle.")
            result.evidence.forEach { println(it) }
        } else {
            println("\nEVIDENCE\n" + result.evidence.joinToString("\n"))
        }
        
        println("\nREALITY MATRIX")
        println("Implementation Confidence: HIGH") 
        println("Configuration Confidence: ${if (result.success) "HIGH" else "LOW"}")
        println("Historical Availability: OBSERVED")
        println("Current Availability: ${dbCap.state}")
        
        println("\nCIRCUIT STATE\n${dbCap.circuitState}")
        println("\nNEXT ACTION\n${if (result.success) "No immediate retry required.\nCapability is eligible for authorized Opportunity Engine work." else "Capability gap identified."}")
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        assertTrue(result.success)
    }
}
