package com.example.ai.capabilities.ecology

import org.junit.Test
import org.junit.Assert.*
import java.time.ZoneId
import java.time.ZonedDateTime

class GeospatialIdentityTest {

    @Test
    fun testIdentityHydrationAndGeospatialContext() {
        val identityEngine = GeospatialIdentityEngine()
        val contextEngine = GeospatialContextEngine(identityEngine)
        
        val birthLocation = GeospatialAnchor(
            localityName = "Cordele, GA",
            precisionLevel = PrecisionLevel.CITY,
            timestamp = ZonedDateTime.of(1996, 11, 12, 0, 0, 0, 0, ZoneId.of("America/New_York")),
            provenance = "Owner Assertion",
            verificationState = IdentityVerificationState.LOCALLY_ASSERTED,
            permittedUses = setOf("ASTROCARTOGRAPHY")
        )
        
        val identity = PhysicalOwnerIdentity(
            identityFacts = mapOf("legalName" to "Marquis Deshaun Whitacre"),
            verifiedAttributes = emptySet(),
            geographicAnchors = listOf(birthLocation),
            explicitPreferences = mapOf("climate" to "TEMPERATE")
        )
        
        identityEngine.hydrateIdentity(identity)
        
        val retrievedIdentity = identityEngine.ownerIdentity.value
        assertNotNull(retrievedIdentity)
        assertEquals("Marquis Deshaun Whitacre", retrievedIdentity?.identityFacts?.get("legalName"))
        assertEquals("Cordele, GA", retrievedIdentity?.geographicAnchors?.first()?.localityName)
        
        val target = GeospatialAnchor(
            latitude = 34.0522,
            longitude = -118.2437,
            localityName = "Los Angeles, CA",
            precisionLevel = PrecisionLevel.EXACT,
            timestamp = ZonedDateTime.now(),
            provenance = "Hypothetical Target",
            verificationState = IdentityVerificationState.UNVERIFIED,
            retentionPolicy = "EPHEMERAL"
        )
        
        val result = contextEngine.evaluateLocation(
            target = target,
            empiricalSource = emptyMap(),
            preferences = emptyMap(),
            symbolicSystems = listOf("astrocartography")
        )
        
        assertEquals(0.85, result.empiricalData.economicIndicators["economic_opportunity"] ?: 0.0, 0.01)
        assertEquals("Jupiter MC line (High visibility/success)", result.symbolicData.first().interpretations.first())
        assertEquals("Recommended for further research", result.inference)
        assertEquals(com.example.ai.capabilities.memory.EpistemicStatus.SYMBOLIC_INTERPRETATION, result.symbolicData.first().epistemicClassification)
        
        println("━━━━━━━━ M. ENGINE — GEOSPATIAL IDENTITY ONTOLOGY ━━━━━━━━")
        println("IDENTITY HYDRATED: ${retrievedIdentity?.identityFacts?.get("legalName")}")
        println("ORIGIN ANCHOR: ${retrievedIdentity?.geographicAnchors?.first()?.localityName} (Precision: ${retrievedIdentity?.geographicAnchors?.first()?.precisionLevel})")
        println("EVALUATION INFERENCE: ${result.inference}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
