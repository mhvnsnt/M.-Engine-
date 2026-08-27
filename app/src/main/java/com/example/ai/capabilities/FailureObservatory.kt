package com.example.ai.capabilities

enum class FailureSource {
    COMPILER, UNIT_TEST, RUNTIME_CRASH, LOGCAT_ERROR, ANR, UI_TEST, USER_REPORT
}

data class FailureEvent(
    val id: String,
    val source: FailureSource,
    val timestamp: Long,
    val rawLog: String,
    val stackTrace: String?,
    val metadata: Map<String, String>
)

data class FailureCluster(
    val clusterId: String,
    val signature: String, // E.g., "NullPointerException at MainActivity.kt:42"
    val severity: IssueSeverity,
    val events: List<FailureEvent>,
    val firstSeen: Long,
    val lastSeen: Long
)

interface FailureObservatory {
    suspend fun ingestFailure(event: FailureEvent)
    suspend fun getActiveClusters(): List<FailureCluster>
    suspend fun correlateToIssue(clusterId: String): DiscoveredIssue
}

class FailureObservatoryImpl : FailureObservatory {
    private val events = mutableListOf<FailureEvent>()
    private val clusters = mutableMapOf<String, FailureCluster>()

    override suspend fun ingestFailure(event: FailureEvent) {
        events.add(event)
        
        // Real implementation would use NLP or stacktrace hashing here
        val signature = event.stackTrace?.lines()?.firstOrNull() ?: event.rawLog.take(50)
        val clusterId = "cluster-\${signature.hashCode()}"
        
        val existing = clusters[clusterId]
        if (existing != null) {
            clusters[clusterId] = existing.copy(
                events = existing.events + event,
                lastSeen = event.timestamp
            )
        } else {
            clusters[clusterId] = FailureCluster(
                clusterId = clusterId,
                signature = signature,
                severity = IssueSeverity.HIGH,
                events = listOf(event),
                firstSeen = event.timestamp,
                lastSeen = event.timestamp
            )
        }
    }

    override suspend fun getActiveClusters(): List<FailureCluster> {
        return clusters.values.toList()
    }

    override suspend fun correlateToIssue(clusterId: String): DiscoveredIssue {
        val cluster = clusters[clusterId] ?: throw IllegalArgumentException("Unknown cluster")
        return DiscoveredIssue(
            id = "issue-\$clusterId",
            category = IssueCategory.RUNTIME,
            severity = cluster.severity,
            description = "Repeated failure: \${cluster.signature} (\${cluster.events.size} occurrences)",
            context = cluster.events.last().rawLog,
            repositoryRef = RepositoryRef("internal", "m-engine")
        )
    }
}
