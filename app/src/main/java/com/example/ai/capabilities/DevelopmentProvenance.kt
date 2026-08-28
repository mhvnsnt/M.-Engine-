package com.example.ai.capabilities

/**
 * Data structures representing M. Engine's Development Provenance Chain:
 *
 * DISCOVERY
 *   ↓
 * Observed deficiency
 *   ↓
 * Evidence establishing deficiency (Pre-fix reproduction)
 *   ↓
 * Research candidates (2024 - 2026 ecosystem)
 *   ↓
 * Candidate benchmarks
 *   ↓
 * Native-vs-external comparison
 *   ↓
 * Decision (KEEP / COMBINE / ADAPT / REPLACE / REJECT)
 *   ↓
 * Implementation
 *   ↓
 * Pre-fix evidence vs Post-fix evidence
 *   ↓
 * Regression created & verified
 *   ↓
 * Security verification
 *   ↓
 * Final capability classification
 */

data class DiscoveryRecord(
    val scanType: String,
    val targetRepo: String,
    val filesInspectedCount: Int,
    val deficienciesFound: List<String>,
    val inspectionTimestamp: Long = System.currentTimeMillis()
)

data class ObservedDeficiency(
    val id: String,
    val componentTarget: String,
    val description: String,
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW
    val isBehavioral: Boolean,
    val failureMode: String
)

data class PreFixEvidenceRecord(
    val reproductionScenario: String,
    val reproductionTestName: String?,
    val failureObserved: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProvenanceResearchCandidate(
    val name: String,
    val repositoryUrl: String,
    val ecosystem: String,
    val releaseYear: Int, // 2024, 2025, 2026
    val architectureSummary: String,
    val maturityScore: Double, // 0.0 to 1.0
    val adoptionScore: Double, // 0.0 to 1.0
    val maintenanceScore: Double, // 0.0 to 1.0
    val license: String
)

data class CandidateBenchmarkResult(
    val candidateName: String,
    val executionLatencyMs: Long,
    val accuracyScore: Double,
    val securityScore: Double,
    val integrationComplexityScore: Double,
    val overallBenchmarkScore: Double
)

data class ComparisonMatrix(
    val nativeStrengths: List<String>,
    val nativeDeficiencies: List<String>,
    val candidateStrengths: List<String>,
    val candidateDeficiencies: List<String>,
    val recommendation: String
)

enum class ProvenanceDecision {
    KEEP,      // Keep existing implementation; external candidates are inferior or unverified
    COMBINE,   // Fuse external patterns/rules with native engine
    ADAPT,     // Adapt external architecture to native Kotlin / Android M3 reality
    REPLACE,   // Fully replace native component with vetted external implementation
    REJECT     // Reject external candidate due to security, license, or benchmark failure
}

data class ProvenanceImplementationRecord(
    val branchName: String,
    val workerUsed: String,
    val filesModified: List<String>,
    val diffSummary: String,
    val commitHash: String? = null
)

data class PostFixEvidenceRecord(
    val verificationScenario: String,
    val verificationTestName: String,
    val verificationOutput: String,
    val buildStatus: String, // BUILD SUCCESSFUL
    val allTestsPassed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class RegressionProofRecord(
    val testClassName: String,
    val testMethodName: String,
    val assertionsEnforced: List<String>,
    val verifiedDurationMs: Long
)

data class SecurityVerificationRecord(
    val sastScanPassed: Boolean,
    val secretsLeakedCount: Int,
    val permissionViolationsCount: Int,
    val scannedFilesCount: Int,
    val auditSummary: String
)

data class CapabilityClassificationRecord(
    val capabilityName: String,
    val maturityTier: String, // EXPERIMENTAL, STABLE, PROVENANCE_LOCKED
    val confidenceScore: Double,
    val evidenceRecordId: String,
    val boundaryClassification: String // FULLY_LOCAL, SANDBOX_GATED, BLOCKED_PHYSICAL_BOUNDARY
)

data class DevelopmentProvenance(
    val id: String,
    val missionId: String,
    val targetRepo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val discovery: DiscoveryRecord,
    val deficiency: ObservedDeficiency,
    val preFixEvidence: PreFixEvidenceRecord,
    val researchCandidates: List<ProvenanceResearchCandidate>,
    val candidateBenchmarks: List<CandidateBenchmarkResult>,
    val nativeVsExternalComparison: ComparisonMatrix,
    val decision: ProvenanceDecision,
    val decisionJustification: String,
    val implementation: ProvenanceImplementationRecord,
    val postFixEvidence: PostFixEvidenceRecord,
    val regressionCreated: RegressionProofRecord,
    val securityVerification: SecurityVerificationRecord,
    val finalCapabilityClassification: CapabilityClassificationRecord
)

interface ProvenanceLedger {
    suspend fun recordProvenance(provenance: DevelopmentProvenance)
    suspend fun getProvenance(id: String): DevelopmentProvenance?
    suspend fun getProvenanceByMission(missionId: String): List<DevelopmentProvenance>
    suspend fun getAllProvenances(): List<DevelopmentProvenance>
    fun exportProvenanceMarkdown(provenance: DevelopmentProvenance): String
}

class InMemoryProvenanceLedger : ProvenanceLedger {
    private val records = mutableMapOf<String, DevelopmentProvenance>()

    override suspend fun recordProvenance(provenance: DevelopmentProvenance) {
        records[provenance.id] = provenance
    }

    override suspend fun getProvenance(id: String): DevelopmentProvenance? {
        return records[id]
    }

    override suspend fun getProvenanceByMission(missionId: String): List<DevelopmentProvenance> {
        return records.values.filter { it.missionId == missionId }
    }

    override suspend fun getAllProvenances(): List<DevelopmentProvenance> {
        return records.values.sortedByDescending { it.timestamp }
    }

    override fun exportProvenanceMarkdown(provenance: DevelopmentProvenance): String {
        return buildString {
            appendLine("# Development Provenance Record: `${provenance.id}`")
            appendLine("**Mission ID:** `${provenance.missionId}` | **Target Repo:** `${provenance.targetRepo}`")
            appendLine("**Timestamp:** ${provenance.timestamp}\n")
            
            appendLine("## 1. Discovery")
            appendLine("- **Scan Type:** ${provenance.discovery.scanType}")
            appendLine("- **Files Inspected:** ${provenance.discovery.filesInspectedCount}")
            appendLine("- **Deficiencies Discovered:** ${provenance.discovery.deficienciesFound.joinToString(", ")}\n")

            appendLine("## 2. Observed Deficiency")
            appendLine("- **Component Target:** `${provenance.deficiency.componentTarget}`")
            appendLine("- **Severity:** ${provenance.deficiency.severity}")
            appendLine("- **Description:** ${provenance.deficiency.description}")
            appendLine("- **Failure Mode:** ${provenance.deficiency.failureMode}\n")

            appendLine("## 3. Pre-Fix Evidence (Reproduction Proof)")
            appendLine("- **Scenario:** ${provenance.preFixEvidence.reproductionScenario}")
            appendLine("- **Test Run:** `${provenance.preFixEvidence.reproductionTestName ?: "Manual Trigger"}`")
            appendLine("- **Observed Failure:** `${provenance.preFixEvidence.failureObserved}` (Exit code: ${provenance.preFixEvidence.exitCode})\n")

            appendLine("## 4. Ecosystem Research & Candidates")
            provenance.researchCandidates.forEach { candidate ->
                appendLine("- **${candidate.name}** (${candidate.releaseYear}, ${candidate.ecosystem}) - License: ${candidate.license}")
                appendLine("  Maturity: ${candidate.maturityScore} | Adoption: ${candidate.adoptionScore} | Maintenance: ${candidate.maintenanceScore}")
                appendLine("  *${candidate.architectureSummary}*")
            }
            appendLine()

            appendLine("## 5. Candidate Benchmarks")
            provenance.candidateBenchmarks.forEach { bench ->
                appendLine("- **${bench.candidateName}**: Latency: ${bench.executionLatencyMs}ms | Accuracy: ${bench.accuracyScore} | Security: ${bench.securityScore} | Overall: ${bench.overallBenchmarkScore}")
            }
            appendLine()

            appendLine("## 6. Native vs. External Comparison")
            appendLine("- **Native Strengths:** ${provenance.nativeVsExternalComparison.nativeStrengths.joinToString(", ")}")
            appendLine("- **Native Deficiencies:** ${provenance.nativeVsExternalComparison.nativeDeficiencies.joinToString(", ")}")
            appendLine("- **Candidate Strengths:** ${provenance.nativeVsExternalComparison.candidateStrengths.joinToString(", ")}")
            appendLine("- **Recommendation:** ${provenance.nativeVsExternalComparison.recommendation}\n")

            appendLine("## 7. Decision & Rational Justification")
            appendLine("- **Decision:** **${provenance.decision.name}**")
            appendLine("- **Justification:** ${provenance.decisionJustification}\n")

            appendLine("## 8. Implementation")
            appendLine("- **Worker Used:** ${provenance.implementation.workerUsed}")
            appendLine("- **Branch:** `${provenance.implementation.branchName}`")
            appendLine("- **Files Modified:** ${provenance.implementation.filesModified.joinToString(", ")}")
            appendLine("- **Diff Summary:** ${provenance.implementation.diffSummary}\n")

            appendLine("## 9. Post-Fix Evidence (Physical Verification)")
            appendLine("- **Verification Test:** `${provenance.postFixEvidence.verificationTestName}`")
            appendLine("- **Build Output:** ${provenance.postFixEvidence.buildStatus}")
            appendLine("- **All Tests Passed:** ${provenance.postFixEvidence.allTestsPassed}")
            appendLine("- **Result Output:** ${provenance.postFixEvidence.verificationOutput}\n")

            appendLine("## 10. Permanent Regression Test")
            appendLine("- **Class:** `${provenance.regressionCreated.testClassName}`")
            appendLine("- **Method:** `${provenance.regressionCreated.testMethodName}`")
            appendLine("- **Assertions:** ${provenance.regressionCreated.assertionsEnforced.joinToString("; ")}\n")

            appendLine("## 11. Security Audit Verification")
            appendLine("- **SAST Passed:** ${provenance.securityVerification.sastScanPassed}")
            appendLine("- **Secrets Leaked:** ${provenance.securityVerification.secretsLeakedCount}")
            appendLine("- **Audit Summary:** ${provenance.securityVerification.auditSummary}\n")

            appendLine("## 12. Final Capability Classification")
            appendLine("- **Capability:** `${provenance.finalCapabilityClassification.capabilityName}`")
            appendLine("- **Maturity Tier:** ${provenance.finalCapabilityClassification.maturityTier}")
            appendLine("- **Confidence Score:** ${provenance.finalCapabilityClassification.confidenceScore}")
            appendLine("- **Boundary Classification:** ${provenance.finalCapabilityClassification.boundaryClassification}")
            appendLine("- **Evidence Record ID:** `${provenance.finalCapabilityClassification.evidenceRecordId}`")
        }
    }
}
