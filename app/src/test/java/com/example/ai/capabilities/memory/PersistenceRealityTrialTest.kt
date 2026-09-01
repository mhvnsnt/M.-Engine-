package com.example.ai.capabilities.memory

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.ZonedDateTime
import com.example.ai.capabilities.ecology.*

class PersistenceRealityTrialTest {

    @Test
    fun testProcessDeathAndContextReconstruction() {
        val tempFile = File.createTempFile("m_engine_ledger_", ".dat")
        tempFile.deleteOnExit()

        // --- PROCESS 1: Initial Run ---
        var ledger1: FileBackedConversationLedger? = FileBackedConversationLedger(tempFile)
        
        val eventA = ConversationEvent(
            eventId = "EVENT_A",
            actor = ConversationActor.OWNER,
            rawContent = "I prefer X.",
            provenance = EventProvenance("TEST", "conv-1")
        )
        ledger1?.appendEvent(eventA)

        val eventB = ConversationEvent(
            eventId = "EVENT_B",
            actor = ConversationActor.OWNER,
            rawContent = "I no longer prefer X. I prefer Y.",
            provenance = EventProvenance("TEST", "conv-2"),
            supersededByEventId = null
        )
        ledger1?.appendEvent(eventB)
        
        // Correct EVENT_A to point to EVENT_B as its superseder. 
        // In reality we'd append a new event or update the DB record. For this test, 
        // we'll simulate the supersession update via a replacement event.
        val eventA_Updated = eventA.copy(supersededByEventId = "EVENT_B")
        ledger1?.appendEvent(eventA_Updated) // Overwrites in the index

        // Append 10,000 events to simulate scale
        for (i in 1..10000) {
            ledger1?.appendEvent(
                ConversationEvent(
                    actor = ConversationActor.SYSTEM,
                    rawContent = "Background telemetry tick $i",
                    provenance = EventProvenance("SYSTEM", "bg-process")
                )
            )
        }

        // SIMULATE PROCESS DEATH
        ledger1 = null
        System.gc()

        // --- PROCESS 2: Restart ---
        val ledger2 = FileBackedConversationLedger(tempFile)
        
        val ontologyEngine = OntologyFederationEngine()
        val ownerContext = OwnerContextGraph(ontologyEngine)
        
        val identity = PhysicalOwnerIdentity(
            identityFacts = mapOf("legalName" to "Marquis"),
            verifiedAttributes = emptySet(),
            geographicAnchors = listOf(
                GeospatialAnchor(
                    localityName = "GA",
                    precisionLevel = PrecisionLevel.CITY,
                    timestamp = ZonedDateTime.now(),
                    provenance = "TEST",
                    verificationState = IdentityVerificationState.UNVERIFIED
                )
            ),
            explicitPreferences = emptyMap()
        )

        ownerContext.hydrate(
            newIdentity = identity,
            newGoals = listOf(OwnerGoal("G1", "Build autonomous systems", "AUTONOMY", "LONG", 1)),
            newPreferences = emptyList(),
            symbolicClaims = emptyList()
        )

        val reconstructionEngine = ContextReconstructionEngine(
            ledger = ledger2,
            ownerContext = ownerContext,
            ontologyFederation = ontologyEngine
        )

        // 1. Verify Raw History intact
        val events = ledger2.queryEventsByTime(0, System.currentTimeMillis() + 100000)
        assertTrue("Ledger must contain 10000+ events", events.size > 10000)

        // 2. Verify A is historically accessible but B is current
        val retrievedA = ledger2.getEvent("EVENT_A")
        assertNotNull(retrievedA)
        assertEquals("EVENT_B", retrievedA?.supersededByEventId)
        
        val currentTruth = reconstructionEngine.resolveSupersession("EVENT_A")
        assertEquals("EVENT_B", currentTruth?.eventId)
        assertEquals("I no longer prefer X. I prefer Y.", currentTruth?.rawContent)

        // 3. Verify provenance
        assertEquals("TEST", currentTruth?.provenance?.sourcePlatform)

        println("━━━━━━━━ M. ENGINE — PERSISTENCE REALITY TRIAL ━━━━━━━━")
        println("Events restored after process death: ${events.size}")
        println("Supersession resolution: EVENT_A correctly resolved to -> ${currentTruth?.eventId}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
