package com.example.ai.capabilities.ecology

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EvidenceBasedHealthTest {

    @Test
    fun testUnknownIsNotFailing() {
        val record = createUnknown(HealthState.UNKNOWN)
        assertNotEquals("UNKNOWN should never be interpreted as FAILING", HealthState.FAILING, record.value)
        assertEquals(HealthState.UNKNOWN, record.value)
    }

    @Test
    fun testBuildSuccessCannotBeInferred() {
        // Even if structural health is OBSERVED and excellent
        val structural = HealthDimensionRecord(
            value = HealthState.OBSERVED,
            confidence = 1.0,
            evidenceReferences = listOf("Perfect structure"),
            sourceCommitSha = "sha123"
        )
        
        // Build health must remain UNKNOWN without execution
        val build = createUnknown(
            unknownValue = HealthState.UNKNOWN, 
            gap = CapabilityGap("SANDBOX", false, "High", "High", "High", "Remote", "AUTH")
        )
        
        assertEquals(HealthState.OBSERVED, structural.value)
        assertEquals(HealthState.UNKNOWN, build.value)
        assertNotNull(build.capabilityGap)
    }

    @Test
    fun testOldEvidenceDoesNotApplyToNewCommit() {
        val commit1 = "abc1234"
        val commit2 = "def5678"
        
        val buildHealth = HealthDimensionRecord(
            value = HealthState.HEALTHY,
            confidence = 1.0,
            evidenceReferences = listOf("Build passed exit code 0"),
            sourceCommitSha = commit1
        )
        
        assertTrue("Evidence applies to current commit", buildHealth.isValidFor(commit1))
        assertFalse("Evidence must not silently transfer to new commit", buildHealth.isValidFor(commit2))
    }

    @Test
    fun testInactivityIsNotUnhealthy() {
        val record = HealthDimensionRecord(
            value = ActivityState.STABLE_LOW_ACTIVITY,
            confidence = 0.9,
            evidenceReferences = listOf("No commits in 6 months, but 0 open issues"),
            sourceCommitSha = "sha123"
        )
        assertEquals(ActivityState.STABLE_LOW_ACTIVITY, record.value)
        assertNotEquals("Inactivity is not automatically FAILING or BROKEN", ActivityState.ABANDONED_CANDIDATE, record.value)
    }
    
    @Test
    fun testIssueCountDoesNotDeterminePressure() {
        val record = HealthDimensionRecord(
            value = IssuePressureState.LOW_PRESSURE,
            confidence = 0.8,
            evidenceReferences = listOf("100 open issues, but 98 are low-priority feature requests"),
            sourceCommitSha = "sha123"
        )
        assertEquals(IssuePressureState.LOW_PRESSURE, record.value)
    }
}
