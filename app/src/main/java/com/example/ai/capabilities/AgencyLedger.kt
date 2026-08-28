package com.example.ai.capabilities

enum class AgencyDecision {
    PROCEED,
    PAUSE_FOR_AUTHORIZATION,
    HALT_SAFETY,
    HALT_RESOURCE_EXHAUSTED,
    HALT_EVIDENCE_FAILED
}

data class AgencyLedgerEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String,
    val authorizationStatus: String,
    val decision: AgencyDecision,
    val decisionReasoning: String,
    val actionTaken: String?,
    val observation: String?,
    val resultStatus: String?,
    val evidenceId: String?,
    val learning: String?,
    val nextDecisionId: String?
)

interface AgencyLedger {
    suspend fun recordEntry(entry: AgencyLedgerEntry): Boolean
    suspend fun getEntriesForIntent(intentSubstring: String): List<AgencyLedgerEntry>
    suspend fun getRecentEntries(limit: Int): List<AgencyLedgerEntry>
}

class InMemoryAgencyLedger : AgencyLedger {
    private val ledger = mutableListOf<AgencyLedgerEntry>()

    override suspend fun recordEntry(entry: AgencyLedgerEntry): Boolean {
        ledger.add(entry)
        return true
    }

    override suspend fun getEntriesForIntent(intentSubstring: String): List<AgencyLedgerEntry> {
        return ledger.filter { it.intent.contains(intentSubstring, ignoreCase = true) }
    }

    override suspend fun getRecentEntries(limit: Int): List<AgencyLedgerEntry> {
        return ledger.sortedByDescending { it.timestamp }.take(limit)
    }
}
