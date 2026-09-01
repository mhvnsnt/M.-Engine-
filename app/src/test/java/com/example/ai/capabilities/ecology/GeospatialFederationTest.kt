package com.example.ai.capabilities.ecology

import org.junit.Test
import org.junit.Assert.*
import java.time.ZonedDateTime
import com.example.ai.capabilities.memory.EpistemicStatus

class GeospatialFederationTest {

    @Test
    fun testGeospatialFederationBoundaries() {
        val identityEngine = GeospatialIdentityEngine()
        val contextEngine = GeospatialContextEngine(identityEngine)
        val symbolicEngine = GeospatialSymbolicEngine()
        val federationEngine = GeospatialFederationEngine(contextEngine, symbolicEngine)
        
        val identity = PhysicalOwnerIdentity(
            identityFacts = mapOf("legalName" to "Marquis"),
            verifiedAttributes = emptySet(),
            geographicAnchors = listOf(
                GeospatialAnchor(
                    latitude = 0.0,
                    longitude = 0.0,
                    localityName = "GA",
                    precisionLevel = PrecisionLevel.CITY,
                    timestamp = ZonedDateTime.now(),
                    provenance = "TEST",
                    verificationState = IdentityVerificationState.UNVERIFIED,
                    permittedUses = setOf("ASTROCARTOGRAPHY")
                )
            ),
            explicitPreferences = emptyMap()
        )
        
        val target = GeospatialAnchor(
            latitude = 34.0522,
            longitude = -118.2437,
            localityName = "Los Angeles, CA",
            precisionLevel = PrecisionLevel.EXACT,
            timestamp = ZonedDateTime.now(),
            provenance = "TEST",
            verificationState = IdentityVerificationState.UNVERIFIED,
            retentionPolicy = "EPHEMERAL",
            permittedUses = setOf("ASTROCARTOGRAPHY")
        )
        
        val synthesis = federationEngine.federateEvaluations(
            target = target,
            identity = identity,
            empiricalSource = emptyMap(),
            preferences = emptyMap(),
            symbolicSystems = listOf("astrocartography")
        )
        
        assertNotNull(synthesis.empiricalEvaluation)
        assertNotNull(synthesis.symbolicCalculations)
        assertTrue(synthesis.epistemicBoundariesPreserved)
        assertEquals(EpistemicStatus.SYMBOLIC_INTERPRETATION, synthesis.symbolicCalculations?.epistemicClassification)
        assertTrue(synthesis.synthesisInference.contains("EMPIRICAL:"))
        assertTrue(synthesis.synthesisInference.contains("SYMBOLIC:"))
        
        println("━━━━━━━━ M. ENGINE — GEOSPATIAL FEDERATION ━━━━━━━━")
        println("TARGET: ${synthesis.targetLocation.localityName}")
        println("SYNTHESIS: ${synthesis.synthesisInference}")
        println("BOUNDARIES PRESERVED: ${synthesis.epistemicBoundariesPreserved}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
