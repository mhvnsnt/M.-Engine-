package com.example.ai.capabilities.ecology

enum class ExecutionState { PENDING, IN_PROGRESS, COMPLETED, FAILED }

object IdempotencyLedger {
    private val ledger = mutableMapOf<String, ExecutionState>()
    
    fun claimExecution(actionId: String): Boolean {
        if (ledger[actionId] == ExecutionState.IN_PROGRESS || ledger[actionId] == ExecutionState.COMPLETED) {
            return false
        }
        ledger[actionId] = ExecutionState.IN_PROGRESS
        return true
    }
    
    fun markCompleted(actionId: String, success: Boolean) {
        ledger[actionId] = if (success) ExecutionState.COMPLETED else ExecutionState.FAILED
    }
    
    fun clear() {
        ledger.clear()
    }
}
