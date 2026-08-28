package com.example.ai.capabilities

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class ProviderMetrics(
    val endpointKey: String,
    val providerType: String,
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageLatencyMs: Long = 0,
    val lastLatencyMs: Long = 0,
    val lastSuccessTimestamp: Long = 0,
    val lastFailureTimestamp: Long = 0,
    val lastError: ProviderErrorInfo? = null,
    val consecutiveFailures: Int = 0,
    val cooldownUntilTimestamp: Long = 0,
    val reliabilityScore: Float = 0.85f
) {
    val isCoolingDown: Boolean
        get() = System.currentTimeMillis() < cooldownUntilTimestamp

    val successRate: Float
        get() = if (totalRequests == 0L) 1.0f else (successfulRequests.toFloat() / totalRequests)
}

class ProviderMetricsTracker {

    private val metricsMap = ConcurrentHashMap<String, ProviderMetrics>()

    fun recordSuccess(endpointKey: String, providerType: String, latencyMs: Long) {
        metricsMap.compute(endpointKey) { _, existing ->
            val prev = existing ?: ProviderMetrics(endpointKey = endpointKey, providerType = providerType)
            val newTotal = prev.totalRequests + 1
            val newSuccess = prev.successfulRequests + 1
            
            // Exponential moving average for latency (smoothing factor = 0.3)
            val newAvgLatency = if (prev.totalRequests == 0L) latencyMs else (prev.averageLatencyMs * 0.7 + latencyMs * 0.3).toLong()
            
            val score = calculateReliabilityScore(
                successCount = newSuccess,
                totalCount = newTotal,
                avgLatency = newAvgLatency,
                consecutiveFailures = 0,
                isCoolingDown = false
            )

            prev.copy(
                totalRequests = newTotal,
                successfulRequests = newSuccess,
                averageLatencyMs = newAvgLatency,
                lastLatencyMs = latencyMs,
                lastSuccessTimestamp = System.currentTimeMillis(),
                consecutiveFailures = 0,
                cooldownUntilTimestamp = 0L,
                reliabilityScore = score
            )
        }
    }

    fun recordFailure(endpointKey: String, providerType: String, error: ProviderErrorInfo) {
        val now = System.currentTimeMillis()
        val cooldownDuration = error.recommendedCooldownMs
        val cooldownUntil = if (cooldownDuration > 0) now + cooldownDuration else 0L

        metricsMap.compute(endpointKey) { _, existing ->
            val prev = existing ?: ProviderMetrics(endpointKey = endpointKey, providerType = providerType)
            val newTotal = prev.totalRequests + 1
            val newFailed = prev.failedRequests + 1
            val newConsecutive = prev.consecutiveFailures + 1

            val isCooling = cooldownUntil > now
            val score = calculateReliabilityScore(
                successCount = prev.successfulRequests,
                totalCount = newTotal,
                avgLatency = prev.averageLatencyMs,
                consecutiveFailures = newConsecutive,
                isCoolingDown = isCooling
            )

            prev.copy(
                totalRequests = newTotal,
                failedRequests = newFailed,
                lastFailureTimestamp = now,
                lastError = error,
                consecutiveFailures = newConsecutive,
                cooldownUntilTimestamp = cooldownUntil,
                reliabilityScore = score
            )
        }
    }

    fun isAvailable(endpointKey: String): Boolean {
        val metric = metricsMap[endpointKey] ?: return true
        val now = System.currentTimeMillis()
        if (metric.cooldownUntilTimestamp > now) return false
        if (metric.lastError?.kind == ProviderErrorKind.AUTH_FAILED) return false
        return true
    }

    fun getReliabilityScore(endpointKey: String): Float {
        val metric = metricsMap[endpointKey] ?: return 0.85f // Neutral starting baseline
        val now = System.currentTimeMillis()
        if (metric.cooldownUntilTimestamp > now) {
            return 0.0f
        }
        return metric.reliabilityScore
    }

    fun getMetrics(endpointKey: String): ProviderMetrics? {
        return metricsMap[endpointKey]
    }

    fun getAllMetrics(): Map<String, ProviderMetrics> {
        return metricsMap.toMap()
    }

    fun resetCooldown(endpointKey: String) {
        metricsMap.computeIfPresent(endpointKey) { _, prev ->
            prev.copy(cooldownUntilTimestamp = 0L, consecutiveFailures = 0)
        }
    }

    fun setCooldown(endpointKey: String, providerType: String, durationMs: Long, reason: String = "Manual cooldown") {
        val now = System.currentTimeMillis()
        val cooldownUntil = now + durationMs
        val error = ProviderErrorInfo(
            kind = ProviderErrorKind.RATE_LIMITED,
            rawMessage = reason,
            recommendedCooldownMs = durationMs,
            isRetryable = true
        )
        metricsMap.compute(endpointKey) { _, existing ->
            val prev = existing ?: ProviderMetrics(endpointKey = endpointKey, providerType = providerType)
            prev.copy(
                lastFailureTimestamp = now,
                lastError = error,
                cooldownUntilTimestamp = cooldownUntil,
                reliabilityScore = 0.0f
            )
        }
    }

    private fun calculateReliabilityScore(
        successCount: Long,
        totalCount: Long,
        avgLatency: Long,
        consecutiveFailures: Int,
        isCoolingDown: Boolean
    ): Float {
        if (isCoolingDown) return 0.0f
        if (totalCount == 0L) return 0.85f

        val successRate = (successCount.toFloat() / totalCount).coerceIn(0.0f, 1.0f)
        
        val latencyScore = when {
            avgLatency <= 0 -> 0.9f
            avgLatency < 800 -> 1.0f
            avgLatency < 2000 -> 0.85f
            avgLatency < 5000 -> 0.65f
            avgLatency < 10000 -> 0.45f
            else -> 0.25f
        }

        val penalty = (consecutiveFailures * 0.25f).coerceAtMost(0.9f)
        val rawScore = (successRate * 0.6f + latencyScore * 0.4f) - penalty
        return rawScore.coerceIn(0.05f, 1.0f)
    }
}
