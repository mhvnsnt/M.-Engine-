package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutonomousSelfDevelopmentTest {

    private lateinit var workerPool: AutonomousWorkerPool
    private lateinit var prioritizationEngine: ImprovementPrioritizationEngine
    private lateinit var provenanceLedger: ProvenanceLedger
    private lateinit var evidenceEngine: EvidenceAssuranceEngine
    private lateinit var contextEngine: PersonalContextEngine
    private lateinit var selfDevEngine: AutonomousSelfDevelopmentEngine

    @Before
    fun setUp() {
        workerPool = AutonomousWorkerPoolImpl()
        prioritizationEngine = ImprovementPrioritizationEngineImpl()
        provenanceLedger = InMemoryProvenanceLedger()
        evidenceEngine = EvidenceAssuranceEngineImpl()
        contextEngine = PersonalContextEngineImpl()

        selfDevEngine = AutonomousSelfDevelopmentEngineImpl(
            workerPool = workerPool,
            prioritizationEngine = prioritizationEngine,
            provenanceLedger = provenanceLedger,
            evidenceEngine = evidenceEngine,
            missionEngine = null,
            contextEngine = contextEngine
        )
    }

    @Test
    fun testAutonomousSelfDevelopment_ExecutesFullMission3Pipeline() = runBlocking {
        val result = selfDevEngine.executeAutonomousSelfDevelopment(
            targetRepo = "mhvnsnt/M.-Engine-",
            endpoints = emptyList()
        )

        assertTrue(result.isSuccess)
        assertEquals("mhvnsnt/M.-Engine-", result.targetRepo)
        assertEquals(10, result.stagesCompleted)
        assertTrue(result.priorityScore > 0)

        // Verify Development Provenance was generated and recorded
        val provenance = result.provenance
        assertNotNull(provenance)
        assertEquals(ProvenanceDecision.COMBINE, provenance.decision)
        assertEquals("com.example.ai.capabilities.SecurityScanner", provenance.deficiency.componentTarget)
        assertEquals("CRITICAL", provenance.deficiency.severity)

        // Verify Pre-fix and Post-fix Evidence
        assertNotNull(provenance.preFixEvidence)
        assertNotNull(provenance.postFixEvidence)
        assertTrue(provenance.postFixEvidence.allTestsPassed)

        // Verify Regression and Security Records
        assertEquals("com.example.ai.capabilities.SecurityScannerRegressionTest", provenance.regressionCreated.testClassName)
        assertTrue(provenance.securityVerification.sastScanPassed)
        assertEquals(0, provenance.securityVerification.secretsLeakedCount)

        // Verify Provenance Ledger has the record
        val saved = provenanceLedger.getProvenance(provenance.id)
        assertNotNull(saved)

        // Verify Evidence Assurance Engine has the claim recorded
        val claimEvaluated = evidenceEngine.evaluateClaim("Autonomous Self-Development")
        assertTrue(claimEvaluated)
    }
}
