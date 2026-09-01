package com.example.ai.capabilities.ecology

data class ExecutionBudget(
    var maxIterations: Int = 10,
    var maxParallelWorkers: Int = 3,
    var maxExecutionTimeMs: Long = 5 * 60 * 1000L, // 5 minutes
    var maxNetworkCalls: Int = 50,
    var maxHighCostModelCalls: Int = 2,
    var maxCostUsd: Double = 1.0,
    var maxToolCalls: Int = 100,
    var maxRiskLevel: String = "L2_BOUNDED",
    val startTimeMs: Long = System.currentTimeMillis()
) {
    fun isExhausted(): Boolean {
        val timeElapsed = System.currentTimeMillis() - startTimeMs
        return maxIterations <= 0 ||
               maxNetworkCalls <= 0 ||
               maxHighCostModelCalls <= 0 ||
               maxToolCalls <= 0 ||
               maxCostUsd <= 0.0 ||
               timeElapsed > maxExecutionTimeMs
    }

    fun consumeIteration() { maxIterations-- }
    
    fun consumeNetworkCall(count: Int = 1) { maxNetworkCalls -= count }
    
    fun consumeModelCall(count: Int = 1) { maxHighCostModelCalls -= count }
    
    fun consumeToolCall(count: Int = 1) { maxToolCalls -= count }

    fun consumeCost(cost: Double) { maxCostUsd -= cost }
    
    fun getRemainingTimeMs(): Long {
        return (maxExecutionTimeMs - (System.currentTimeMillis() - startTimeMs)).coerceAtLeast(0L)
    }
}

typealias AutonomousCycleBudget = ExecutionBudget
