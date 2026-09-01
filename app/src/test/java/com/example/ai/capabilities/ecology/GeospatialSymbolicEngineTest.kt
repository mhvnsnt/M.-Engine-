package com.example.ai.capabilities.ecology

import org.junit.Test
import org.junit.Assert.*
import java.time.ZonedDateTime

class GeospatialSymbolicEngineTest {

    @Test
    fun testMathematicalOffsetCalculation() {
        val symbolicEngine = GeospatialSymbolicEngine()
        
        val identity = PhysicalOwnerIdentity(
            identityFacts = mapOf("legalName" to "Marquis"),
            verifiedAttributes = emptySet(),
            geographicAnchors = emptyList(),
            explicitPreferences = emptyMap()
        )
        
        val exactTarget = GeospatialAnchor(
            latitude = 34.0522, // Los Angeles
            longitude = -118.2437,
            localityName = "Los Angeles, CA",
            precisionLevel = PrecisionLevel.EXACT,
            timestamp = ZonedDateTime.now(),
            provenance = "TEST",
            verificationState = IdentityVerificationState.UNVERIFIED,
            retentionPolicy = "EPHEMERAL"
        )
        
        val result = symbolicEngine.computeSymbolicGeospatialOffsets(exactTarget, identity)
        
        // Mathematical Assertions
        assertTrue("Must calculate at least one offset line", result.offsets.isNotEmpty())
        val jupiterOffset = result.offsets.first()
        assertTrue("Distance should be approximately 184km based on the 2-degree longitude mock", 
                   jupiterOffset.distanceToLineKm > 180.0 && jupiterOffset.distanceToLineKm < 190.0)
        assertTrue("Orb must be considered active under 500km", jupiterOffset.isOrbActive)
        
        // Epistemic Classification Assertion
        assertEquals(com.example.ai.capabilities.memory.EpistemicStatus.SYMBOLIC_INTERPRETATION, result.epistemicClassification)
        assertTrue(result.interpretations.first().contains("Jupiter MC"))
        
        println("━━━━━━━━ M. ENGINE — SYMBOLIC GEOSPATIAL COMPUTATION ━━━━━━━━")
        println("TARGET: ${exactTarget.localityName}")
        println("CALCULATED DISTANCE: ${jupiterOffset.distanceToLineKm.toInt()} km to ${jupiterOffset.nearestLines.first().celestialBody} ${jupiterOffset.nearestLines.first().angle}")
        println("ORB ACTIVE: ${jupiterOffset.isOrbActive}")
        println("SYMBOLIC INTERPRETATION: ${result.interpretations.first()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    @Test
    fun testPrivacyBoundsEnforcement() {
        val symbolicEngine = GeospatialSymbolicEngine()
        
        val identity = PhysicalOwnerIdentity(
            identityFacts = emptyMap(),
            verifiedAttributes = emptySet(),
            geographicAnchors = emptyList(),
            explicitPreferences = emptyMap()
        )
        
        val cityTarget = GeospatialAnchor(
            localityName = "Los Angeles, CA",
            precisionLevel = PrecisionLevel.CITY, // NO EXACT LAT/LON
            timestamp = ZonedDateTime.now(),
            provenance = "TEST",
            verificationState = IdentityVerificationState.UNVERIFIED,
            retentionPolicy = "EPHEMERAL"
        )
        
        val result = symbolicEngine.computeSymbolicGeospatialOffsets(cityTarget, identity)
        
        assertTrue("Engine must refuse to calculate mathematical offset without exact coordinates", result.offsets.isEmpty())
        assertTrue("Engine must yield an observation of the privacy bound", result.interpretations.first().contains("Privacy bounds enforced"))
        assertEquals(com.example.ai.capabilities.memory.EpistemicStatus.EMPIRICALLY_VERIFIED, result.epistemicClassification)
        
        println("━━━━━━━━ M. ENGINE — PRIVACY BOUNDS ENFORCEMENT ━━━━━━━━")
        println("TARGET PRECISION: ${cityTarget.precisionLevel}")
        println("RESULT: ${result.interpretations.first()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
