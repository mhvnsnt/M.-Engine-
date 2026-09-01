package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Level 2 — Project authority.
 *
 * Projects sit ABOVE conversations. Before this, a conversation was the highest
 * organisational boundary in the running app, so everything about an initiative
 * — its repositories, decisions, artifacts and worker activity — died with the
 * chat it happened in. That is the gap this table closes.
 *
 * A project is not deleted when work on it stops; it is archived. The Level 0
 * ledger already guarantees the raw events survive, and a project row that
 * vanished would orphan them.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    /** ACTIVE | PAUSED | ARCHIVED */
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Generic association between a project and something that belongs to it.
 *
 * A single association table rather than one join table per relationship: the
 * set of things a project owns (conversations, repositories, missions, jobs,
 * artifacts) is expected to grow, and each new kind should not require a schema
 * migration. `kind` names the relationship; `refId` is the other side's id in
 * its own table.
 */
@Entity(
    tableName = "project_associations",
    primaryKeys = ["projectId", "kind", "refId"],
    indices = [Index("projectId"), Index("kind"), Index("refId")],
)
data class ProjectAssociationEntity(
    val projectId: String,
    /** CONVERSATION | REPOSITORY | MISSION | JOB | ARTIFACT | BRANCH */
    val kind: String,
    val refId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Level 2 project memory — durable decisions, constraints and findings.
 *
 * Deliberately provenance-bearing and supersedable, exactly like Level 0 and
 * Level 1. A project summary must never become the authority over the raw
 * events it was derived from, so every entry carries `sourceEventIds` pointing
 * back into the conversation ledger.
 */
@Entity(
    tableName = "project_memory",
    indices = [Index("projectId"), Index("kind")],
)
data class ProjectMemoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    /** DECISION | CONSTRAINT | GOAL | OPEN_PROBLEM | VERIFIED_FACT | ARCHITECTURE */
    val kind: String,
    val statement: String,
    /** EXPLICIT | OBSERVED | INFERRED | WORKER_REPORTED | VERIFIED */
    val provenance: String = "OBSERVED",
    /** Comma-separated Level 0 event ids this was derived from. */
    val sourceEventIds: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val supersededBy: String? = null,
)

/**
 * The Library — a canonical artifact record.
 *
 * Worker output must not disappear into a log line or an unnamed directory. An
 * artifact is identified by the hash of its content, so the same bytes produced
 * twice are recognisably the same artifact, and a claim that references one can
 * be checked against what was actually produced.
 *
 * `uri` locates the bytes; this table does not store them. Where the bytes live
 * is a storage decision (local file, object store) that must not change the
 * identity of the artifact.
 */
@Entity(
    tableName = "artifacts",
    indices = [Index("contentHash"), Index("projectId"), Index("conversationId"), Index("jobId")],
)
data class ArtifactEntity(
    @PrimaryKey val id: String,
    /** SHA-256 of the content. The artifact's identity, not its location. */
    val contentHash: String,
    /** CODE | BUILD | APK | TEST_REPORT | LOG | SCREENSHOT | IMAGE | VIDEO | AUDIO | DATASET | MODEL | DOCUMENT | OTHER */
    val kind: String,
    val name: String,
    val uri: String,
    val sizeBytes: Long = 0,
    val projectId: String? = null,
    val conversationId: String? = null,
    /** Worker/job that produced it, when it was produced rather than supplied. */
    val jobId: String? = null,
    /** OWNER_SUPPLIED | WORKER_PRODUCED | BUILD_OUTPUT | EXTERNAL_FETCH */
    val provenance: String = "WORKER_PRODUCED",
    val createdAt: Long = System.currentTimeMillis(),
    /** Points at the artifact that replaces this one. History is not deleted. */
    val supersededBy: String? = null,
)

/**
 * A worker execution record, associated with a project.
 *
 * Keeps the distinction the reality contract requires: `reportedResult` is what
 * the worker SAID, `verifiedResult` is what was independently established. They
 * are separate columns on purpose — collapsing them is how a worker's claim
 * silently becomes a fact.
 */
@Entity(
    tableName = "worker_jobs",
    indices = [Index("projectId"), Index("status")],
)
data class WorkerJobEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val conversationId: String? = null,
    val capabilityType: String,
    val providerId: String,
    val objective: String,
    /** PENDING | RUNNING | REPORTED | VERIFIED | FAILED | CANCELLED | CAPABILITY_GAP */
    val status: String = "PENDING",
    /** What the worker claimed. Never promoted to fact on its own. */
    val reportedResult: String? = null,
    /** What was independently established. Null until evidence supports it. */
    val verifiedResult: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
)
