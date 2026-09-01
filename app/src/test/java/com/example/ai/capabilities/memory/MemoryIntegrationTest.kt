package com.example.ai.capabilities.memory

import org.junit.Test
import org.junit.Assert.*
import com.example.ai.capabilities.ecology.*
import java.time.ZonedDateTime

class MemoryIntegrationTest {

    @Test
    fun testImmutableLedgerAppending() {
        val ledger = InMemoryConversationLedger()
        
        val event = ConversationEvent(
            actor = ConversationActor.OWNER,
            rawContent = "I want to focus on Agentic Autonomy.",
            provenance = EventProvenance("TEST_SUITE", "conv-123")
        )
        
        ledger.appendEvent(event)
        
        val retrieved = ledger.getEvent(event.eventId)
        assertNotNull(retrieved)
        assertEquals("I want to focus on Agentic Autonomy.", retrieved?.rawContent)
    }

    @Test
    fun testOwnerContextAndOntologyFederation() {
        val ontologyEngine = OntologyFederationEngine()
        val contextGraph = OwnerContextGraph(ontologyEngine)

        val identity = PhysicalOwnerIdentity(
            identityFacts = mapOf("legalName" to "Marquis Deshaun Whitacre"),
            verifiedAttributes = emptySet(),
            geographicAnchors = listOf(
                GeospatialAnchor(
                    localityName = "Cordele, GA",
                    precisionLevel = PrecisionLevel.CITY,
                    timestamp = ZonedDateTime.now(),
                    provenance = "TEST",
                    verificationState = IdentityVerificationState.UNVERIFIED
                )
            ),
            explicitPreferences = emptyMap()
        )

        // Hydrate instead of depending on hardcoded values
        contextGraph.hydrate(
            newIdentity = identity,
            newGoals = listOf(OwnerGoal("G1", "Material abundance", "ABUNDANCE", "LONG_TERM", 1)),
            newPreferences = listOf(TerminologyPreference("Sovereignty", "Agentic Autonomy", "M. Engine Logic")),
            symbolicClaims = listOf(
                OntologyClaim("numerology", "Marquis", "Life Path 3", EpistemicStatus.SYMBOLIC_INTERPRETATION),
                OntologyClaim("astro_western", "Marquis", "MC: Taurus", EpistemicStatus.SYMBOLIC_INTERPRETATION)
            )
        )
        
        assertEquals("Marquis Deshaun Whitacre", contextGraph.identity?.identityFacts?.get("legalName"))
        
        // Verify Terminology preferences
        val preferred = contextGraph.getPreferredTerminology("Sovereignty")
        assertTrue(preferred!!.contains("Agentic Autonomy"))
        
        // Verify Goals
        val abundanceGoals = contextGraph.getGoalsByCategory("ABUNDANCE")
        assertTrue(abundanceGoals.isNotEmpty())
        
        // Verify Epistemic Separation in Ontology Claims
        val symbolicClaims = ontologyEngine.synthesizeInsights("Marquis")
            .filter { it.epistemicStatus == EpistemicStatus.SYMBOLIC_INTERPRETATION }
            
        assertTrue(symbolicClaims.any { it.ontologyId == "numerology" })
        assertTrue(symbolicClaims.any { it.ontologyId == "astro_western" })
        
        // Ensure the MC in Taurus made it in
        val astrologyClaim = symbolicClaims.first { it.ontologyId == "astro_western" }
        assertTrue(astrologyClaim.claimValue.contains("MC: Taurus"))
    }

    @Test
    fun testGeospatialEvaluation() {
        val identityEngine = GeospatialIdentityEngine()
        val geoEngine = GeospatialContextEngine(identityEngine)
        
        val target = GeospatialAnchor(
            localityName = "Seattle, WA",
            precisionLevel = PrecisionLevel.CITY,
            timestamp = ZonedDateTime.now(),
            provenance = "TEST",
            verificationState = IdentityVerificationState.UNVERIFIED
        )
        
        val result = geoEngine.evaluateLocation(
            target = target,
            empiricalSource = emptyMap(),
            preferences = emptyMap(),
            symbolicSystems = listOf("astrocartography")
        )
        
        assertEquals(0.85, result.empiricalData.economicIndicators["economic_opportunity"] ?: 0.0, 0.01)
        assertEquals("Jupiter MC line (High visibility/success)", result.symbolicData.first().interpretations.first())
        assertEquals("Recommended for further research", result.inference)
        assertEquals(EpistemicStatus.SYMBOLIC_INTERPRETATION, result.symbolicData.first().epistemicClassification)
    }
}
