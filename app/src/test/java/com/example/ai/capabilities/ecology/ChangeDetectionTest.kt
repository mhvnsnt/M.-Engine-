package com.example.ai.capabilities.ecology

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class ChangeDetectionTest {

    private val changeEngine = ChangeDetectionEngine()
    private val planner = ReinspectionPlanner()

    @Test
    fun testCommitAEvidenceDoesNotAutomaticallyTransferToCommitB() {
        val oldCommit = "commitA"
        val newCommit = "commitB"
        
        val oldBuildRecord = HealthDimensionRecord(
            value = HealthState.HEALTHY,
            confidence = 1.0,
            evidenceReferences = listOf("Build success on commitA"),
            sourceCommitSha = oldCommit,
            evidenceStatus = EvidenceStatus.CURRENT
        )
        
        val delta = changeEngine.detectChanges(oldCommit, newCommit, listOf("src/main/App.kt"))
        
        val (newRecord, decision) = changeEngine.transferEvidence(oldBuildRecord, delta, "BuildHealth")
        
        // 1. Old record is preserved as historical
        assertEquals(EvidenceStatus.HISTORICAL, oldBuildRecord.evidenceStatus)
        
        // 2. New record requires revalidation (it's for a new commit)
        assertEquals(EvidenceTransferDecision.REQUIRES_REVALIDATION, decision)
        assertEquals(EvidenceStatus.STALE, newRecord.evidenceStatus)
        assertEquals(newCommit, newRecord.sourceCommitSha)
        assertTrue("Confidence is penalized until physically re-verified", newRecord.confidence < 1.0)
    }

    @Test
    fun testReadmeOnlyChangesDoNotInvalidateStructuralEvidence() {
        val oldCommit = "commitA"
        val newCommit = "commitB"
        
        val oldStructuralRecord = HealthDimensionRecord(
            value = HealthState.OBSERVED,
            confidence = 0.95,
            evidenceReferences = listOf("Valid boundaries"),
            sourceCommitSha = oldCommit,
            evidenceStatus = EvidenceStatus.CURRENT
        )
        
        // Only README changed
        val delta = changeEngine.detectChanges(oldCommit, newCommit, listOf("README.md"))
        
        val (newRecord, decision) = changeEngine.transferEvidence(oldStructuralRecord, delta, "StructuralHealth")
        
        assertEquals(EvidenceTransferDecision.TRANSFERRED_WITH_HIGH_CONFIDENCE, decision)
        assertEquals(EvidenceStatus.CURRENT, newRecord.evidenceStatus)
        assertEquals(0.95, newRecord.confidence, 0.001)
    }

    @Test
    fun testDependencyConfigTriggersDependencyRevalidation() {
        val oldCommit = "commitA"
        val newCommit = "commitB"
        
        val oldDepRecord = HealthDimensionRecord(
            value = DependencyState.CURRENT,
            confidence = 0.9,
            evidenceReferences = listOf("Checked yesterday"),
            sourceCommitSha = oldCommit,
            evidenceStatus = EvidenceStatus.CURRENT
        )
        
        val delta = changeEngine.detectChanges(oldCommit, newCommit, listOf("package.json"))
        val (newRecord, decision) = changeEngine.transferEvidence(oldDepRecord, delta, "DependencyFreshness")
        
        assertEquals(EvidenceTransferDecision.REQUIRES_REVALIDATION, decision)
        assertEquals(EvidenceStatus.STALE, newRecord.evidenceStatus)
    }

    @Test
    fun testNetworkFailureProducesUnknownNotNoChange() {
        val delta = changeEngine.detectChanges("commitA", "commitB", listOf("src/App.kt"), networkSuccess = false)
        assertFalse(delta.successful)
        assertEquals(0, delta.changes.size)
        assertEquals("Network inspection failure or missing commit", delta.failureReason)
        
        val oldRecord = HealthDimensionRecord(HealthState.HEALTHY, 1.0, listOf(), "commitA")
        val (newRecord, decision) = changeEngine.transferEvidence(oldRecord, delta, "BuildHealth")
        
        assertEquals(EvidenceTransferDecision.NOT_TRANSFERABLE, decision)
        assertEquals(0.0, newRecord.confidence, 0.0)
    }

    @Test
    fun testHighConsequenceChangeReceivesHigherReinspectionPriority() {
        val lowImpactDelta = changeEngine.detectChanges("commitA", "commitB", listOf("README.md", "docs/API.md"))
        val highImpactDelta = changeEngine.detectChanges("commitA", "commitB", listOf(
            "package.json", "src/App.kt", "src/Auth.kt", "src/Database.kt", "src/Net.kt",
            "src/A.kt", "src/B.kt", "src/C.kt", "src/D.kt", "src/E.kt", "src/F.kt"
        )) // > 10 files, includes deps & source
        
        val lowPriority = planner.planReinspection(lowImpactDelta)
        val highPriority = planner.planReinspection(highImpactDelta)
        
        assertEquals(ReinspectionAction.NO_ACTION, lowPriority)
        assertEquals(ReinspectionAction.FULL_REINSPECTION, highPriority)
    }
}
