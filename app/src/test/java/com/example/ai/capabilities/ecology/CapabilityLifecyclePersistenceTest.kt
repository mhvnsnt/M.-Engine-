package com.example.ai.capabilities.ecology

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CapabilityStateDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class CapabilityLifecyclePersistenceTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CapabilityStateDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.capabilityStateDao()
        FederatedCapabilityRegistry.reset()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCompleteEvidenceLifecycle() = runBlocking {
        println("━━━━━━━━ M. ENGINE — EVIDENCE LIFECYCLE ━━━━━━━━\n")
        
        // 1. live probe
        val cap = FederatedCapabilityRegistry.getCapability("GitHubWorkerCapability") as GitHubWorkerCapability
        println("1. Executing live probe on ${cap.name}...")
        val health = cap.verifyHealth()
        
        println("Probe Result: ${health.verifiedState} (Success=${health.success})")
        
        // 2. evidence persisted & capability transition persisted
        println("2. Persisting capability transitions...")
        FederatedCapabilityRegistry.persistState(dao)
        
        val savedStates = dao.getAllStates()
        assertTrue(savedStates.any { it.capabilityId == cap.capabilityId })
        println("Evidence and transition successfully written to SQLite Ledger.")
        
        // 3. control-plane restart
        println("3. Simulating Control Plane Restart...")
        FederatedCapabilityRegistry.reset()
        val capAfterRestart = FederatedCapabilityRegistry.getCapability("GitHubWorkerCapability") as GitHubWorkerCapability
        assertEquals(CapabilityState.IMPLEMENTED_UNVERIFIED, capAfterRestart.state)
        
        // 4. state recovered
        println("4. Recovering state from SQLite Ledger...")
        FederatedCapabilityRegistry.restoreState(dao)
        
        val capRecovered = FederatedCapabilityRegistry.getCapability("GitHubWorkerCapability") as GitHubWorkerCapability
        
        // 5. Observatory displays recovered evidence
        println("\n5. Observatory Recovery View:")
        println("CAPABILITY: ${capRecovered.name}")
        println("STATE: ${capRecovered.state}")
        println("EVIDENCE:")
        capRecovered.verificationEvidence.forEach {
            println("  - $it")
        }
        
        assertEquals(CapabilityState.AVAILABLE, capRecovered.state)
        assertTrue(capRecovered.verificationEvidence.isNotEmpty())
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
