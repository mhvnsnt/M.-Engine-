package com.example.ai.capabilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImprovementPrioritizationEngineTest {

    private lateinit var engine: ImprovementPrioritizationEngine

    @Before
    fun setUp() {
        engine = ImprovementPrioritizationEngineImpl()
    }

    @Test
    fun testPrioritizationFormula_CalculatesNetValueScoreCorrectly() {
        val highValueCandidate = ImprovementCandidate(
            id = "c1",
            title = "High Value Security Fix",
            componentTarget = "SecurityScanner",
            description = "Blocks leaked credentials",
            impact = 10.0,
            confidence = 1.0,
            feasibility = 1.0,
            evidenceQuality = 1.0,
            userValue = 10.0,
            risk = 1.0,
            complexity = 1.0,
            regressionPotential = 1.0
        )

        val scored = engine.scoreCandidate(highValueCandidate)

        // Utility = 10 * 1 * 1 * 1 * 10 = 100.0
        // Risk = 1 * 1 * 1 = 1.0
        // Net Value = 99.0
        assertEquals(99.0, scored.rawValueScore, 0.01)
        assertTrue(scored.isExecutableInCurrentEnvironment)
        assertEquals("FULLY_LOCAL_EXECUTABLE", scored.boundaryClassification)
    }

    @Test
    fun testRealityBoundary_BlocksCandidateRequiringExternalHardware() {
        val hardwareCandidate = ImprovementCandidate(
            id = "c2",
            title = "Physical Accelerometer Sensor Calibration",
            componentTarget = "HardwareActuator",
            description = "Requires physical Android sensor rig",
            impact = 9.0,
            confidence = 0.9,
            feasibility = 1.0,
            evidenceQuality = 1.0,
            userValue = 8.0,
            risk = 2.0,
            complexity = 2.0,
            regressionPotential = 2.0,
            externalHardwareRequired = true // Blocked by physical boundary
        )

        val scored = engine.scoreCandidate(hardwareCandidate)

        // Feasibility forced to 0.0 due to missing hardware
        // Utility = 9 * 0.9 * 0 * 1 * 8 = 0.0
        // Risk = 2 * 2 * 2 = 8.0
        // Net Value = -8.0
        assertEquals(-8.0, scored.rawValueScore, 0.01)
        assertFalse("Must not be executable when physical hardware is missing", scored.isExecutableInCurrentEnvironment)
        assertEquals("BLOCKED_PHYSICAL_HARDWARE", scored.boundaryClassification)
        assertTrue(scored.prioritizationRankingReason.contains("BLOCKED"))
    }

    @Test
    fun testRealityBoundary_BlocksCandidateRequiringMissingCredentials() {
        val cloudCandidate = ImprovementCandidate(
            id = "c3",
            title = "AWS S3 Multi-Region Cloud Replication",
            componentTarget = "CloudSync",
            description = "Requires AWS IAM credentials",
            impact = 8.0,
            confidence = 0.8,
            feasibility = 1.0,
            evidenceQuality = 1.0,
            userValue = 7.0,
            risk = 1.0,
            complexity = 1.0,
            regressionPotential = 1.0,
            missingCredentialsRequired = true // Missing credentials
        )

        val scored = engine.scoreCandidate(cloudCandidate)

        assertFalse("Must not be executable when credentials are missing", scored.isExecutableInCurrentEnvironment)
        assertEquals("BLOCKED_MISSING_CREDENTIALS", scored.boundaryClassification)
    }

    @Test
    fun testRanking_PrioritizesHighValueExecutableOverBlockedOrLowValue() {
        val candidates = listOf(
            ImprovementCandidate("c_low", "Low Value Refactor", "Util", "", 2.0, 0.5, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0),
            ImprovementCandidate("c_blocked", "Hardware Lab", "Sensor", "", 10.0, 1.0, 1.0, 1.0, 10.0, 1.0, 1.0, 1.0, externalHardwareRequired = true),
            ImprovementCandidate("c_high", "Core Security & Worker Pool", "Core", "", 9.0, 0.9, 1.0, 1.0, 9.0, 1.0, 1.0, 1.0)
        )

        val ranked = engine.rankCandidates(candidates)
        assertEquals("c_high", ranked.first().candidate.id)
        assertTrue(ranked.first().isExecutableInCurrentEnvironment)

        val highest = engine.selectHighestValueExecutableCandidate(candidates)
        assertNotNull(highest)
        assertEquals("c_high", highest?.candidate?.id)
    }
}
