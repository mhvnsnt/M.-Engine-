package com.example.ai

import android.util.Log

enum class FailureCategory {
    COMPILER_ERROR,
    TEST_FAILURE,
    CRASH,
    ANR,
    LOGCAT_EXCEPTION,
    UI_FAILURE,
    GAME_STATE_FAILURE,
    PERFORMANCE_REGRESSION,
    MEMORY_PROBLEM,
    NETWORK_FAILURE,
    DEPENDENCY_PROBLEM,
    SECURITY_FINDING,
    FLAKY_TEST,
    USER_REPORTED
}

data class FailureCluster(
    val id: String,
    val category: FailureCategory,
    val primarySignature: String,
    val occurrences: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val hypothesis: String? = null
)

class FailureObservatory {
    private val clusters = mutableMapOf<String, FailureCluster>()

    fun observeFailure(category: FailureCategory, signature: String, rawLogs: String) {
        val clusterId = generateClusterId(category, signature)
        val existing = clusters[clusterId]
        
        if (existing != null) {
            clusters[clusterId] = existing.copy(
                occurrences = existing.occurrences + 1,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            clusters[clusterId] = FailureCluster(
                id = clusterId,
                category = category,
                primarySignature = signature,
                occurrences = 1,
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis()
            )
        }
        
        Log.e("FailureObservatory", "Observed failure [$category]: $signature (Occurrences: ${clusters[clusterId]?.occurrences})")
    }

    private fun generateClusterId(category: FailureCategory, signature: String): String {
        return "${category.name}_${signature.hashCode()}"
    }

    fun getTopClusters(): List<FailureCluster> {
        return clusters.values.sortedByDescending { it.occurrences }
    }
}
