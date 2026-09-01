package com.example.ai.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LedgerRepositoryTest {

    @Test
    fun testSQLiteLedgerCapabilitiesAndVerification() {
        val ledger = SQLiteLedgerRepository("jdbc:sqlite::memory:")
        ledger.initDatabase()

        val caps = ledger.getCapabilities()
        assertTrue(caps.size >= 9)

        // Verify capability
        val verifyRes = ledger.verifyCapability("GitHubWorkerCapability")
        assertTrue(verifyRes["success"] as Boolean)
        assertEquals("AVAILABLE", verifyRes["state"])

        // Toggle capability
        val toggleRes = ledger.toggleCapability("GitHubWorkerCapability", false)
        assertEquals(false, toggleRes["isEnabled"])
        assertEquals(false, toggleRes["available"])
    }

    @Test
    fun testCycleBudgetAndWorkerStreamGovernance() {
        val ledger = SQLiteLedgerRepository("jdbc:sqlite::memory:")
        ledger.initDatabase()

        ledger.startCycle("test-cycle-1", "run-1")
        assertEquals("STARTED", ledger.getCycleStatus("test-cycle-1"))

        val active = ledger.getActiveCycle()
        assertNotNull(active)
        assertEquals("test-cycle-1", active["cycleId"])

        val cancelSuccess = ledger.cancelCycle("test-cycle-1")
        assertTrue(cancelSuccess)

        val telem = ledger.getTelemetry()
        assertNotNull(telem["lastHeartbeat"])
    }

    @Test
    fun testTandemSignalRecording() {
        val ledger = SQLiteLedgerRepository("jdbc:sqlite::memory:")
        ledger.initDatabase()

        val signal = ledger.recordDevelopmentSignal("NEW_REQUIREMENT", "physics-core", "Test root-motion blending")
        assertEquals("NEW_REQUIREMENT", signal["type"])
        assertEquals("physics-core", signal["project"])

        val tandem = ledger.getTandemDevelopment()
        assertNotNull(tandem["humanSignals"])
    }
}
