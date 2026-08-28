package com.example.ai.capabilities

enum class FailureSource {
    COMPILER, UNIT_TEST, RUNTIME_CRASH, LOGCAT_ERROR, ANR, UI_TEST, REGRESSION_FAIL, USER_REPORT
}

data class FailureEvent(
    val id: String,
    val source: FailureSource,
    val timestamp: Long,
    val rawLog: String,
    val stackTrace: String?,
    val metadata: Map<String, String> = emptyMap(),
    val affectedComponent: String? = null
)

data class FailureCluster(
    val clusterId: String,
    val signature: String, // E.g., "NullPointerException at MainActivity.kt:42"
    val severity: IssueSeverity,
    val events: List<FailureEvent>,
    val firstSeen: Long,
    val lastSeen: Long,
    val targetComponent: String? = null,
    val priorityScore: Double = 0.0
)

data class AutonomousFailureMission(
    val missionId: String,
    val clusterId: String,
    val title: String,
    val targetComponent: String,
    val failureSignature: String,
    val priorityScore: Double,
    val reproductionScenario: String,
    val requiredEvidenceLevel: EvidenceLevel = EvidenceLevel.REGRESSION_PROOF
)

interface FailureObservatory {
    suspend fun ingestFailure(event: FailureEvent)
    suspend fun getActiveClusters(): List<FailureCluster>
    suspend fun correlateToIssue(clusterId: String): DiscoveredIssue
    suspend fun generateAutonomousMission(clusterId: String): AutonomousFailureMission?
    suspend fun generateTopRankedMissions(limit: Int = 5): List<AutonomousFailureMission>
}

class FailureObservatoryImpl(
    private val repoGraphEngine: RepositoryGraphEngine? = null
) : FailureObservatory {
    private val events = mutableListOf<FailureEvent>()
    private val clusters = mutableMapOf<String, FailureCluster>()

    override suspend fun ingestFailure(event: FailureEvent) {
        events.add(event)

        val firstStackLine = event.stackTrace?.lines()?.firstOrNull { it.contains("at ") || it.contains("Exception") || it.contains("Error") }
        val signature = firstStackLine ?: event.rawLog.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: "Unknown Failure"
        val clusterId = "cluster-${Math.abs(signature.hashCode())}"

        // Extract target component from stacktrace or log
        val targetComponent = event.affectedComponent ?: extractComponentFromStack(event.stackTrace ?: event.rawLog)

        val existing = clusters[clusterId]
        val occurrences = (existing?.events?.size ?: 0) + 1

        // Value Prioritization Formula: (Impact * Confidence * Feasibility * EvidenceQuality * UserValue) / (1 + Risk + Complexity + RegressionPotential)
        val impact = when (event.source) {
            FailureSource.RUNTIME_CRASH, FailureSource.ANR -> 5.0
            FailureSource.COMPILER -> 4.5
            FailureSource.REGRESSION_FAIL, FailureSource.UNIT_TEST -> 4.0
            FailureSource.UI_TEST -> 3.5
            else -> 3.0
        }
        val priority = ((impact * 4.0 * 4.5 * 4.0 * 4.0) / (1.0 + 1.0 + 2.0 + 1.0)) * (1.0 + Math.min(occurrences, 10) * 0.1)

        val severity = when {
            impact >= 4.5 -> IssueSeverity.CRITICAL
            impact >= 3.5 -> IssueSeverity.HIGH
            else -> IssueSeverity.MEDIUM
        }

        if (existing != null) {
            clusters[clusterId] = existing.copy(
                events = existing.events + event,
                lastSeen = event.timestamp,
                priorityScore = priority,
                targetComponent = targetComponent ?: existing.targetComponent
            )
        } else {
            clusters[clusterId] = FailureCluster(
                clusterId = clusterId,
                signature = signature,
                severity = severity,
                events = listOf(event),
                firstSeen = event.timestamp,
                lastSeen = event.timestamp,
                targetComponent = targetComponent,
                priorityScore = priority
            )
        }
    }

    override suspend fun getActiveClusters(): List<FailureCluster> {
        return clusters.values.sortedByDescending { it.priorityScore }
    }

    override suspend fun correlateToIssue(clusterId: String): DiscoveredIssue {
        val cluster = clusters[clusterId] ?: throw IllegalArgumentException("Unknown cluster $clusterId")
        return DiscoveredIssue(
            id = "issue-$clusterId",
            category = IssueCategory.RUNTIME,
            severity = cluster.severity,
            description = "Repeated failure: ${cluster.signature} (${cluster.events.size} occurrences)",
            context = cluster.events.last().rawLog,
            repositoryRef = RepositoryRef("internal", "m-engine")
        )
    }

    override suspend fun generateAutonomousMission(clusterId: String): AutonomousFailureMission? {
        val cluster = clusters[clusterId] ?: return null
        val target = cluster.targetComponent ?: "UnknownTarget"
        return AutonomousFailureMission(
            missionId = "miss-repair-${cluster.clusterId}",
            clusterId = cluster.clusterId,
            title = "Autonomous Fix: ${cluster.signature.take(60)}",
            targetComponent = target,
            failureSignature = cluster.signature,
            priorityScore = cluster.priorityScore,
            reproductionScenario = "Reproduce failure in $target with input: ${cluster.events.last().rawLog.take(120)}",
            requiredEvidenceLevel = EvidenceLevel.REGRESSION_PROOF
        )
    }

    override suspend fun generateTopRankedMissions(limit: Int): List<AutonomousFailureMission> {
        return clusters.values
            .sortedByDescending { it.priorityScore }
            .take(limit)
            .mapNotNull { generateAutonomousMission(it.clusterId) }
    }

    private fun extractComponentFromStack(stack: String): String? {
        val classRegex = Regex("""([A-Za-z0-9_]+\.kt|[A-Za-z0-9_]+\.java)""")
        return classRegex.find(stack)?.groupValues?.get(1)
    }
}
