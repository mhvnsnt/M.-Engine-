package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DevelopmentProvenanceTest {

    private lateinit var ledger: ProvenanceLedger

    @Before
    fun setUp() {
        ledger = InMemoryProvenanceLedger()
    }

    @Test
    fun testDevelopmentProvenance_RecordsAndExportsCompleteChain() = runBlocking {
        val provenance = DevelopmentProvenance(
            id = "prov-test-1",
            missionId = "mission-003",
            targetRepo = "mhvnsnt/M.-Engine-",
            discovery = DiscoveryRecord("AST_SCAN", "mhvnsnt/M.-Engine-", 42, listOf("Deficiency A")),
            deficiency = ObservedDeficiency("def-1", "SecurityScanner", "Lacks secret scanning", "CRITICAL", true, "Passes leaked keys"),
            preFixEvidence = PreFixEvidenceRecord("Scan leaked key", "testPreFix", "Passed without error", 0),
            researchCandidates = listOf(
                ProvenanceResearchCandidate("Gitleaks", "https://github.com/gitleaks/gitleaks", "Go", 2025, "Regex engine", 0.95, 0.98, 0.96, "MIT")
            ),
            candidateBenchmarks = listOf(
                CandidateBenchmarkResult("Gitleaks", 12L, 0.98, 0.99, 0.2, 0.92)
            ),
            nativeVsExternalComparison = ComparisonMatrix(
                listOf("Fast"), listOf("Missing patterns"), listOf("Comprehensive"), listOf("External"), "COMBINE"
            ),
            decision = ProvenanceDecision.COMBINE,
            decisionJustification = "Fuses patterns into native Kotlin engine without external binary dependency.",
            implementation = ProvenanceImplementationRecord("feature/mission-3", "Autonomous Kotlin/AST Coder", listOf("SecurityScanner.kt"), "Added regex token scanning"),
            postFixEvidence = PostFixEvidenceRecord("Rescan leaked key", "testPostFix", "Blocked leaked key", "BUILD SUCCESSFUL", true),
            regressionCreated = RegressionProofRecord("SecurityScannerRegressionTest", "testSecurityScanner_BlocksAnthropicAndGeminiKeyLeaks", listOf("assertFalse(result.passed)"), 100L),
            securityVerification = SecurityVerificationRecord(true, 0, 0, 42, "Clean SAST audit"),
            finalCapabilityClassification = CapabilityClassificationRecord("AUTONOMOUS_SECURITY_SCANNER", "PROVENANCE_LOCKED", 1.0, "ev-123", "FULLY_LOCAL_EXECUTABLE")
        )

        ledger.recordProvenance(provenance)

        val retrieved = ledger.getProvenance("prov-test-1")
        assertNotNull(retrieved)
        assertEquals("mission-003", retrieved?.missionId)
        assertEquals(ProvenanceDecision.COMBINE, retrieved?.decision)

        // Export Markdown
        val markdown = ledger.exportProvenanceMarkdown(provenance)
        assertTrue(markdown.contains("# Development Provenance Record: `prov-test-1`"))
        assertTrue(markdown.contains("## 1. Discovery"))
        assertTrue(markdown.contains("## 2. Observed Deficiency"))
        assertTrue(markdown.contains("## 3. Pre-Fix Evidence (Reproduction Proof)"))
        assertTrue(markdown.contains("## 4. Ecosystem Research & Candidates"))
        assertTrue(markdown.contains("## 5. Candidate Benchmarks"))
        assertTrue(markdown.contains("## 6. Native vs. External Comparison"))
        assertTrue(markdown.contains("## 7. Decision & Rational Justification"))
        assertTrue(markdown.contains("## 8. Implementation"))
        assertTrue(markdown.contains("## 9. Post-Fix Evidence (Physical Verification)"))
        assertTrue(markdown.contains("## 10. Permanent Regression Test"))
        assertTrue(markdown.contains("## 11. Security Audit Verification"))
        assertTrue(markdown.contains("## 12. Final Capability Classification"))
    }
}
