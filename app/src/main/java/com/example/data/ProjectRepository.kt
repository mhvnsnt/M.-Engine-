package com.example.data

import com.example.ai.capabilities.memory.ConversationActor
import com.example.ai.capabilities.memory.ConversationEvent
import com.example.ai.capabilities.memory.EventProvenance
import java.security.MessageDigest
import java.util.UUID

/**
 * Canonical API for the Project authority and the Library.
 *
 * Two rules are enforced here rather than left to callers:
 *
 *  1. Every structural change emits a Level 0 event. A project created, an
 *     artifact registered or a worker result recorded is operational history,
 *     and history that only exists as a row cannot be traced back to why it
 *     happened.
 *
 *  2. A worker's claim never becomes a verified fact in one step.
 *     [recordWorkerReport] writes `reportedResult` and moves the job to
 *     REPORTED. Only [verifyWorkerResult], which requires supporting artifact
 *     ids, writes `verifiedResult` and moves it to VERIFIED.
 */
/**
 * Characters of a UUID kept in a generated id. Enough to stay unique across a
 * single owner's projects while remaining readable in a log line.
 */
private const val ID_SUFFIX_LENGTH = 8

/**
 * Where an artifact belongs. Grouped rather than passed as four more
 * parameters because these always travel together and are all optional
 * linkage — none of them is part of the artifact's identity, which is its
 * content hash.
 */
data class ArtifactLinkage(
    val projectId: String? = null,
    val conversationId: String? = null,
    val jobId: String? = null,
    val provenance: String = "WORKER_PRODUCED",
)

class ProjectRepository(
    private val dao: ProjectDao,
    private val ledger: RoomConversationLedger? = null,
) {

    private suspend fun emit(actor: ConversationActor, content: String, conversationId: String) {
        ledger?.appendSuspending(
            ConversationEvent(
                actor = actor,
                rawContent = content,
                provenance = EventProvenance(
                    sourcePlatform = "ANDROID",
                    conversationId = conversationId,
                ),
            ),
        )
    }

    // --- projects ----------------------------------------------------------

    suspend fun createProject(title: String, description: String = ""): ProjectEntity {
        val project = ProjectEntity(
            id = "proj-" + UUID.randomUUID().toString().take(ID_SUFFIX_LENGTH),
            title = title,
            description = description,
        )
        dao.upsertProject(project)
        emit(ConversationActor.SYSTEM, "PROJECT_CREATED ${project.id} $title", project.id)
        return project
    }

    suspend fun getProject(id: String) = dao.getProject(id)

    suspend fun activeProjects() = dao.activeProjects()

    /** Archives rather than deletes — see ProjectDao.archiveProject. */
    suspend fun archiveProject(id: String) {
        dao.archiveProject(id)
        emit(ConversationActor.SYSTEM, "PROJECT_ARCHIVED $id", id)
    }

    // --- associations ------------------------------------------------------

    suspend fun associateConversation(projectId: String, conversationId: String) =
        link(projectId, "CONVERSATION", conversationId)

    suspend fun associateRepository(projectId: String, repository: String) =
        link(projectId, "REPOSITORY", repository)

    suspend fun associateMission(projectId: String, missionId: String) =
        link(projectId, "MISSION", missionId)

    private suspend fun link(projectId: String, kind: String, refId: String) {
        dao.associate(ProjectAssociationEntity(projectId, kind, refId))
        emit(ConversationActor.SYSTEM, "PROJECT_ASSOCIATED $projectId $kind $refId", projectId)
    }

    suspend fun conversationsOf(projectId: String) =
        dao.associationsOfKind(projectId, "CONVERSATION").map { it.refId }

    suspend fun repositoriesOf(projectId: String) =
        dao.associationsOfKind(projectId, "REPOSITORY").map { it.refId }

    /** Which project a conversation belongs to, if any. */
    suspend fun projectForConversation(conversationId: String): ProjectEntity? =
        dao.findOwningProject("CONVERSATION", conversationId)?.let { dao.getProject(it.projectId) }

    // --- Level 2 project memory -------------------------------------------

    /**
     * Records a durable project fact. `sourceEventIds` ties it back to the raw
     * Level 0 events it came from; a derived statement with no provenance is
     * exactly the kind of summary that must never outrank raw history.
     */
    suspend fun rememberForProject(
        projectId: String,
        kind: String,
        statement: String,
        provenance: String = "OBSERVED",
        sourceEventIds: List<String> = emptyList(),
    ): ProjectMemoryEntity {
        val entry = ProjectMemoryEntity(
            id = "pm-" + UUID.randomUUID().toString().take(ID_SUFFIX_LENGTH),
            projectId = projectId,
            kind = kind,
            statement = statement,
            provenance = provenance,
            sourceEventIds = sourceEventIds.joinToString(","),
        )
        dao.upsertMemory(entry)
        return entry
    }

    suspend fun projectMemory(projectId: String) = dao.activeMemory(projectId)

    /** Corrects a project fact without deleting the superseded statement. */
    suspend fun supersedeProjectMemory(oldId: String, replacement: ProjectMemoryEntity) {
        dao.upsertMemory(replacement)
        dao.supersedeMemory(oldId, replacement.id)
    }

    // --- Library -----------------------------------------------------------

    /**
     * Registers an artifact. Identity is the SHA-256 of its content, so the
     * same bytes registered twice are recognisably the same artifact even if
     * they were written to different paths.
     */
    suspend fun registerArtifact(
        name: String,
        kind: String,
        uri: String,
        content: ByteArray,
        linkage: ArtifactLinkage = ArtifactLinkage(),
    ): ArtifactEntity {
        val projectId = linkage.projectId
        val conversationId = linkage.conversationId
        val hash = sha256(content)
        val artifact = ArtifactEntity(
            id = "art-" + UUID.randomUUID().toString().take(ID_SUFFIX_LENGTH),
            contentHash = hash,
            kind = kind,
            name = name,
            uri = uri,
            sizeBytes = content.size.toLong(),
            projectId = projectId,
            conversationId = conversationId,
            jobId = linkage.jobId,
            provenance = linkage.provenance,
        )
        dao.upsertArtifact(artifact)
        projectId?.let { dao.associate(ProjectAssociationEntity(it, "ARTIFACT", artifact.id)) }
        emit(
            ConversationActor.SYSTEM,
            "ARTIFACT_REGISTERED ${artifact.id} $kind $name sha256=$hash",
            projectId ?: conversationId ?: "global",
        )
        return artifact
    }

    suspend fun artifactsForProject(projectId: String) = dao.artifactsForProject(projectId)
    suspend fun artifactsForConversation(conversationId: String) =
        dao.artifactsForConversation(conversationId)
    suspend fun artifactsByHash(hash: String) = dao.artifactsByHash(hash)
    suspend fun recentArtifacts(limit: Int = 50) = dao.recentArtifacts(limit)

    // --- worker jobs -------------------------------------------------------

    suspend fun startJob(
        projectId: String?,
        capabilityType: String,
        providerId: String,
        objective: String,
        conversationId: String? = null,
    ): WorkerJobEntity {
        val job = WorkerJobEntity(
            id = "job-" + UUID.randomUUID().toString().take(ID_SUFFIX_LENGTH),
            projectId = projectId,
            conversationId = conversationId,
            capabilityType = capabilityType,
            providerId = providerId,
            objective = objective,
            status = "RUNNING",
        )
        dao.upsertJob(job)
        projectId?.let { dao.associate(ProjectAssociationEntity(it, "JOB", job.id)) }
        emit(
            ConversationActor.SYSTEM,
            "JOB_DISPATCHED ${job.id} $capabilityType via $providerId: $objective",
            projectId ?: "global",
        )
        return job
    }

    /**
     * Records what the worker SAID. Status becomes REPORTED, never VERIFIED —
     * a self-report is not evidence.
     */
    suspend fun recordWorkerReport(jobId: String, reported: String) {
        val job = dao.getJob(jobId) ?: return
        dao.upsertJob(
            job.copy(
                reportedResult = reported,
                status = "REPORTED",
                finishedAt = System.currentTimeMillis(),
            ),
        )
        emit(ConversationActor.WORKER, "WORKER_REPORTED_RESULT $jobId: $reported", job.projectId ?: "global")
    }

    /**
     * Promotes a reported result to verified — but only with supporting
     * artifacts. Refuses otherwise, because a verification with nothing behind
     * it is indistinguishable from the worker's own claim.
     *
     * Returns false when the promotion was refused.
     */
    suspend fun verifyWorkerResult(
        jobId: String,
        verified: String,
        supportingArtifactIds: List<String>,
    ): Boolean {
        if (supportingArtifactIds.isEmpty()) return false
        val job = dao.getJob(jobId) ?: return false
        // Every cited artifact must actually exist.
        if (supportingArtifactIds.any { dao.getArtifact(it) == null }) return false

        dao.upsertJob(job.copy(verifiedResult = verified, status = "VERIFIED"))
        emit(
            ConversationActor.SYSTEM,
            "RESULT_VERIFIED $jobId evidence=${supportingArtifactIds.joinToString(",")}: $verified",
            job.projectId ?: "global",
        )
        return true
    }

    suspend fun jobsForProject(projectId: String) = dao.jobsForProject(projectId)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
