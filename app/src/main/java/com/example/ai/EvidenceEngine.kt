package com.example.ai

import android.util.Log

enum class EvidenceType {
    SOURCE_ANALYSIS,
    COMPILER_OUTPUT,
    UNIT_TEST,
    INTEGRATION_TEST,
    RUNTIME_BEHAVIOR,
    VIDEO_EVIDENCE,
    REGRESSION_TEST,
    EXTERNAL_RESEARCH
}

enum class EvidenceVerdict {
    UNTRUSTED,
    VERIFIED_FAIL,
    VERIFIED_PASS,
    INCONCLUSIVE
}

enum class EvidenceLevel { 
    MODEL_CLAIM, 
    STATIC_ANALYSIS, 
    UNIT_TEST, 
    INTEGRATION_TEST, 
    BEHAVIORAL_EVIDENCE, 
    TEMPORAL_MULTIMODAL_EVIDENCE, 
    HUMAN_VERIFIED 
}

data class EvidenceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val commitSha: String,
    val type: EvidenceType,
    val level: EvidenceLevel,
    val description: String,
    val commandOrAction: String,
    val expectedResult: String,
    val observedResult: String,
    val verdict: EvidenceVerdict,
    val reproducibility: String = "N/A",
    val confidence: Float = 0.0f,
    val isStale: Boolean = false
)

class EvidenceEngine {
    private val records = mutableListOf<EvidenceRecord>()

    fun recordEvidence(
        commitSha: String,
        type: EvidenceType,
        level: EvidenceLevel,
        description: String,
        commandOrAction: String,
        expectedResult: String,
        observedResult: String,
        verdict: EvidenceVerdict,
        reproducibility: String = "N/A",
        confidence: Float = 1.0f
    ) {
        val record = EvidenceRecord(
            commitSha = commitSha,
            type = type,
            level = level,
            description = description,
            commandOrAction = commandOrAction,
            expectedResult = expectedResult,
            observedResult = observedResult,
            verdict = verdict,
            reproducibility = reproducibility,
            confidence = confidence
        )
        records.add(record)
        Log.d("EvidenceEngine", "Recorded Evidence [$verdict] for $commitSha: $description")
    }

    fun invalidateStaleEvidence(newCommitSha: String) {
        for (i in records.indices) {
            if (records[i].commitSha != newCommitSha && !records[i].isStale) {
                records[i] = records[i].copy(isStale = true)
                Log.d("EvidenceEngine", "Marked evidence ${records[i].id} as STALE (SHA mismatch)")
            }
        }
    }

    fun getRecords(): List<EvidenceRecord> = records.toList()

    fun evaluateGoal(goalDescription: String, currentCommitSha: String): Boolean {
        // A goal is only met if there is VERIFIED_PASS evidence for the behavioral aspects,
        // and no VERIFIED_FAIL evidence that is more recent.
        // It must also NOT be stale (i.e. tied to the current commit).
        val relevantRecords = records.filter { 
            it.description.contains(goalDescription, ignoreCase = true) && !it.isStale && it.commitSha == currentCommitSha 
        }
        val latestFail = relevantRecords.findLast { it.verdict == EvidenceVerdict.VERIFIED_FAIL }
        val latestPass = relevantRecords.findLast { it.verdict == EvidenceVerdict.VERIFIED_PASS }

        if (latestPass != null) {
            if (latestFail == null || latestPass.timestamp > latestFail.timestamp) {
                return true
            }
        }
        return false
    }
}
