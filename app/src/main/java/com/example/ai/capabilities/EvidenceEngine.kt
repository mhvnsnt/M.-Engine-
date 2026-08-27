package com.example.ai.capabilities

enum class EvidenceType {
    SOURCE_ANALYSIS,
    COMPILER,
    STATIC_ANALYSIS,
    UNIT_TEST,
    INTEGRATION_TEST,
    RUNTIME_LOG,
    CRASH,
    PERFORMANCE_TRACE,
    UI_INTERACTION,
    VIDEO_OBSERVATION,
    ACTUATOR_RESULT,
    GITHUB_HISTORY,
    DOCUMENTATION,
    BENCHMARK,
    EXTERNAL_RESEARCH,
    USER_REPORT
}

enum class EvidenceLevel(val value: Int) {
    MODEL_CLAIM(0), // "I think this works."
    STATIC_EVIDENCE(1), // "Code analysis indicates this should work."
    AUTOMATED_TEST(2), // "Unit/integration test passes."
    RUNTIME_EVIDENCE(3), // "Executed successfully."
    BEHAVIORAL_EVIDENCE(4), // "Actually interacted with the application."
    TEMPORAL_MULTIMODAL_EVIDENCE(5), // "Observed correct behavior across entire interaction/video."
    INDEPENDENT_VERIFICATION(6), // "Separate verifier reproduced and confirmed the result."
    REGRESSION_PROOF(7) // "Previous failures reproduce as fixed and existing functionality remains intact."
}

enum class EvidenceStatus {
    VALID,
    STALE,
    REQUIRES_REVALIDATION
}

data class StructuredClaim(
    val scenario: String,
    val seed: String?,
    val durationMs: Long,
    val beforeState: String,
    val changeCommit: String,
    val afterState: String,
    val confidence: EvidenceLevel
)

data class EvidenceRecord(
    val id: String,
    val claim: StructuredClaim,
    val evidenceType: EvidenceType,
    val level: EvidenceLevel,
    val source: String,
    val timestamp: Long,
    val reproductionSteps: List<String>,
    val observedResult: String,
    val expectedResult: String?,
    val confidenceScore: Double,
    val independentlyVerified: Boolean,
    var status: EvidenceStatus = EvidenceStatus.VALID
)

interface EvidenceAssuranceEngine {
    suspend fun recordEvidence(record: EvidenceRecord)
    suspend fun evaluateClaim(claimId: String): Boolean
    suspend fun getEvidenceForClaim(claimId: String): List<EvidenceRecord>
    suspend fun requireLevel(claimId: String, requiredLevel: EvidenceLevel): Boolean
    suspend fun markStale(claimId: String)
}

class EvidenceAssuranceEngineImpl : EvidenceAssuranceEngine {
    private val evidenceLedger = mutableListOf<EvidenceRecord>()

    override suspend fun recordEvidence(record: EvidenceRecord) {
        // Enforce Reality Check weighting policy: Model Assertions = 0
        if (record.level == EvidenceLevel.MODEL_CLAIM) {
            // Log warning internally, but we can accept it as Level 0
        }
        evidenceLedger.add(record)
    }

    override suspend fun evaluateClaim(claimId: String): Boolean {
        val evidence = getEvidenceForClaim(claimId).filter { it.status == EvidenceStatus.VALID }
        if (evidence.isEmpty()) return false
        
        val highestLevel = evidence.maxByOrNull { it.level.value }?.level ?: EvidenceLevel.MODEL_CLAIM
        val verified = evidence.any { it.independentlyVerified }
        
        // Reality Check Gate
        return highestLevel.value >= EvidenceLevel.RUNTIME_EVIDENCE.value || verified
    }

    override suspend fun getEvidenceForClaim(claimId: String): List<EvidenceRecord> {
        return evidenceLedger.filter { it.claim.scenario == claimId }
    }

    override suspend fun requireLevel(claimId: String, requiredLevel: EvidenceLevel): Boolean {
        val evidence = getEvidenceForClaim(claimId).filter { it.status == EvidenceStatus.VALID }
        return evidence.any { it.level.value >= requiredLevel.value }
    }
    
    override suspend fun markStale(claimId: String) {
        evidenceLedger.forEach {
            if (it.claim.scenario == claimId) {
                it.status = EvidenceStatus.STALE
            }
        }
    }
}
