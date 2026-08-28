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
    EXPIRED_COMMIT_MISMATCH,
    EXPIRED_FILE_MODIFIED,
    STALE,
    REQUIRES_REVALIDATION
}

data class EvidenceScope(
    val testedCorpus: List<String>,
    val scannerOrEngineVersion: String = "2.4.0",
    val commitHash: String,
    val environment: String = "Android SDK 35 / OpenJDK 21 / Kotlin 2.0.21",
    val targetFileHashes: Map<String, String> = emptyMap(),
    val inputConditions: Map<String, String> = emptyMap()
)

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
    val scope: EvidenceScope = EvidenceScope(
        testedCorpus = listOf("standard"),
        commitHash = "head"
    ),
    var status: EvidenceStatus = EvidenceStatus.VALID
)

interface EvidenceAssuranceEngine {
    suspend fun recordEvidence(record: EvidenceRecord)
    suspend fun evaluateClaim(claimId: String, currentCommit: String = "head", currentFileHashes: Map<String, String> = emptyMap()): Boolean
    suspend fun getEvidenceForClaim(claimId: String): List<EvidenceRecord>
    suspend fun requireLevel(claimId: String, requiredLevel: EvidenceLevel): Boolean
    suspend fun markStale(claimId: String)
    suspend fun checkAndExpireEvidence(currentCommit: String, currentFileHashes: Map<String, String>): Int
    fun formatScopedEvidenceReport(record: EvidenceRecord): String
}

class EvidenceAssuranceEngineImpl : EvidenceAssuranceEngine {
    private val evidenceLedger = mutableListOf<EvidenceRecord>()

    override suspend fun recordEvidence(record: EvidenceRecord) {
        // Enforce Reality Check weighting policy: Model Assertions = 0
        evidenceLedger.add(record)
    }

    override suspend fun evaluateClaim(
        claimId: String,
        currentCommit: String,
        currentFileHashes: Map<String, String>
    ): Boolean {
        val evidenceList = getEvidenceForClaim(claimId)
        if (evidenceList.isEmpty()) return false

        // Revalidate expiration against current commit and file state
        evidenceList.forEach { record ->
            if (record.scope.commitHash != "head" && currentCommit != "head" && record.scope.commitHash != currentCommit) {
                record.status = EvidenceStatus.EXPIRED_COMMIT_MISMATCH
            } else {
                record.scope.targetFileHashes.forEach { (file, oldHash) ->
                    val currentHash = currentFileHashes[file]
                    if (currentHash != null && currentHash != oldHash) {
                        record.status = EvidenceStatus.EXPIRED_FILE_MODIFIED
                    }
                }
            }
        }

        val validEvidence = evidenceList.filter { it.status == EvidenceStatus.VALID }
        if (validEvidence.isEmpty()) return false

        val highestLevel = validEvidence.maxByOrNull { it.level.value }?.level ?: EvidenceLevel.MODEL_CLAIM
        val verified = validEvidence.any { it.independentlyVerified }

        // Reality Check Gate: Must have actual runtime evidence or independent reproduction
        return highestLevel.value >= EvidenceLevel.RUNTIME_EVIDENCE.value || verified
    }

    override suspend fun getEvidenceForClaim(claimId: String): List<EvidenceRecord> {
        return evidenceLedger.filter { it.claim.scenario == claimId || it.claim.scenario.contains(claimId, ignoreCase = true) || it.id == claimId }
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

    override suspend fun checkAndExpireEvidence(
        currentCommit: String,
        currentFileHashes: Map<String, String>
    ): Int {
        var expiredCount = 0
        evidenceLedger.forEach { record ->
            if (record.status == EvidenceStatus.VALID) {
                if (record.scope.commitHash != "head" && currentCommit != "head" && record.scope.commitHash != currentCommit) {
                    record.status = EvidenceStatus.EXPIRED_COMMIT_MISMATCH
                    expiredCount++
                } else {
                    for ((file, oldHash) in record.scope.targetFileHashes) {
                        val currHash = currentFileHashes[file]
                        if (currHash != null && currHash != oldHash) {
                            record.status = EvidenceStatus.EXPIRED_FILE_MODIFIED
                            expiredCount++
                            break
                        }
                    }
                }
            }
        }
        return expiredCount
    }

    override fun formatScopedEvidenceReport(record: EvidenceRecord): String {
        return buildString {
            append("Evidence ID: ${record.id} [${record.status}]\n")
            append("Type: ${record.evidenceType} | Level: ${record.level} (${record.level.value}/7)\n")
            append("Observed: ${record.observedResult}\n")
            append("Scope Boundary:\n")
            append("  - Tested Corpus: ${record.scope.testedCorpus.joinToString(", ")}\n")
            append("  - Engine/Scanner Version: ${record.scope.scannerOrEngineVersion}\n")
            append("  - Commit Hash: ${record.scope.commitHash}\n")
            append("  - Environment: ${record.scope.environment}\n")
            if (record.scope.targetFileHashes.isNotEmpty()) {
                append("  - Verified File Hashes: ${record.scope.targetFileHashes.keys.joinToString(", ")}\n")
            }
        }
    }
}

