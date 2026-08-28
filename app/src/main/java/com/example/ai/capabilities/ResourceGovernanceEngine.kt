package com.example.ai.capabilities

data class ResourceBudget(
    var moneySpentCents: Double = 0.0,
    var moneyAllowedCents: Double = 0.50,
    var tokensUsed: Long = 0,
    var tokensAllowed: Long = 100_000,
    var executionTimeMs: Long = 0,
    var maxExecutionTimeMs: Long = 60_000,
    var cpuUsagePercent: Double = 0.0,
    var batteryImpactLevel: String = "LOW",
    var networkBytesUsed: Long = 0,
    var riskLevel: String = "LOW"
)

enum class ResourceConstraint {
    MONEY_EXHAUSTED,
    TIME_EXHAUSTED,
    TOKENS_EXHAUSTED,
    RISK_TOO_HIGH,
    OK
}

interface ResourceGovernanceEngine {
    fun evaluateAction(estimatedCostCents: Double, estimatedTimeMs: Long, estimatedRisk: String): ResourceConstraint
    fun consumeResources(costCents: Double, timeMs: Long, tokens: Long)
    fun setLimits(money: Double, timeMs: Long, tokens: Long)
    fun getStatus(): ResourceBudget
}

class ResourceGovernanceEngineImpl : ResourceGovernanceEngine {
    private val currentBudget = ResourceBudget()

    override fun evaluateAction(estimatedCostCents: Double, estimatedTimeMs: Long, estimatedRisk: String): ResourceConstraint {
        if (currentBudget.moneySpentCents + estimatedCostCents > currentBudget.moneyAllowedCents) {
            return ResourceConstraint.MONEY_EXHAUSTED
        }
        if (currentBudget.executionTimeMs + estimatedTimeMs > currentBudget.maxExecutionTimeMs) {
            return ResourceConstraint.TIME_EXHAUSTED
        }
        if (estimatedRisk == "CRITICAL" || estimatedRisk == "HIGH") {
            return ResourceConstraint.RISK_TOO_HIGH
        }
        return ResourceConstraint.OK
    }

    override fun consumeResources(costCents: Double, timeMs: Long, tokens: Long) {
        currentBudget.moneySpentCents += costCents
        currentBudget.executionTimeMs += timeMs
        currentBudget.tokensUsed += tokens
    }

    override fun setLimits(money: Double, timeMs: Long, tokens: Long) {
        currentBudget.moneyAllowedCents = money
        currentBudget.maxExecutionTimeMs = timeMs
        currentBudget.tokensAllowed = tokens
    }

    override fun getStatus(): ResourceBudget {
        return currentBudget.copy()
    }
}
