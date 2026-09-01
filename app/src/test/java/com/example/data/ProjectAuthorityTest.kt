package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies the Project authority and Library against the directive's checklist:
 * create a project, associate a conversation, restart persistence, reload,
 * associate artifacts with both project and conversation, attach worker
 * activity, and confirm provenance survives.
 *
 * Uses a REAL file-backed database that is genuinely closed and reopened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ProjectAuthorityTest {

    private fun dbFile() = File.createTempFile("mengine-project-test", ".db").also { it.delete() }

    private fun open(file: File): AppDatabase =
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            file.absolutePath,
        ).addMigrations(*ALL_MIGRATIONS).build()

    private fun repoFor(db: AppDatabase) =
        ProjectRepository(db.projectDao(), RoomConversationLedger(db.conversationEventDao()))

    @Test
    fun `a project and its whole graph survive a persistence restart`() = runTest {
        val file = dbFile()

        // --- session 1 --------------------------------------------------------
        var db = open(file)
        var projects = repoFor(db)

        val project = projects.createProject("Bannon", "Wrestling game")
        projects.associateConversation(project.id, "7")
        projects.associateRepository(project.id, "mhvnsnt/Bannon")

        val job = projects.startJob(
            projectId = project.id,
            capabilityType = "CODING",
            providerId = "openhands_primary",
            objective = "diagnose animation failure",
            conversationId = "7",
        )
        val artifact = projects.registerArtifact(
            name = "build.log",
            kind = "LOG",
            uri = "file:///tmp/build.log",
            content = "BUILD FAILED".toByteArray(),
            projectId = project.id,
            conversationId = "7",
            jobId = job.id,
        )
        projects.rememberForProject(
            projectId = project.id,
            kind = "OPEN_PROBLEM",
            statement = "animations do not trigger on the character model",
            sourceEventIds = listOf("msg-1", "msg-2"),
        )
        db.close()

        // --- session 2: genuinely reopened -----------------------------------
        db = open(file)
        projects = repoFor(db)

        val reloaded = projects.getProject(project.id)
        assertNotNull("the project must survive a restart", reloaded)
        assertEquals("Bannon", reloaded!!.title)

        // A project outlives the conversation it was created in.
        assertEquals(listOf("7"), projects.conversationsOf(project.id))
        assertEquals(listOf("mhvnsnt/Bannon"), projects.repositoriesOf(project.id))

        // Reverse lookup: which project owns this conversation.
        assertEquals(project.id, projects.projectForConversation("7")?.id)

        // The artifact is reachable from the project AND the conversation.
        val byProject = projects.artifactsForProject(project.id)
        val byConversation = projects.artifactsForConversation("7")
        assertEquals(1, byProject.size)
        assertEquals(1, byConversation.size)
        assertEquals(artifact.id, byProject.first().id)

        // Content hash is the identity, and it survived.
        assertEquals(64, byProject.first().contentHash.length)
        assertEquals(artifact.contentHash, byProject.first().contentHash)
        assertEquals(job.id, byProject.first().jobId)

        // Level 2 project memory survived, with provenance back to Level 0.
        val memory = projects.projectMemory(project.id)
        assertEquals(1, memory.size)
        assertEquals("msg-1,msg-2", memory.first().sourceEventIds)

        // Worker activity is attached to the project.
        assertEquals(1, projects.jobsForProject(project.id).size)

        db.close()
        file.delete()
    }

    @Test
    fun `identical content registered twice is recognised as the same artifact`() = runTest {
        val file = dbFile()
        val db = open(file)
        val projects = repoFor(db)
        val project = projects.createProject("Hashing")

        val a = projects.registerArtifact(
            "out.txt", "LOG", "file:///a/out.txt", "same bytes".toByteArray(), project.id,
        )
        val b = projects.registerArtifact(
            "copy.txt", "LOG", "file:///b/copy.txt", "same bytes".toByteArray(), project.id,
        )

        // Different rows and different locations, but one identity.
        assertTrue(a.id != b.id)
        assertEquals(a.contentHash, b.contentHash)
        assertEquals(2, projects.artifactsByHash(a.contentHash).size)

        // Different bytes must not collide.
        val c = projects.registerArtifact(
            "other.txt", "LOG", "file:///c", "different".toByteArray(), project.id,
        )
        assertTrue(c.contentHash != a.contentHash)

        db.close()
        file.delete()
    }

    @Test
    fun `a worker claim cannot become a verified fact without evidence`() = runTest {
        val file = dbFile()
        val db = open(file)
        val projects = repoFor(db)
        val dao = db.projectDao()
        val project = projects.createProject("Verification")

        val job = projects.startJob(project.id, "CODING", "openhands_primary", "fix the build")

        projects.recordWorkerReport(job.id, "I fixed the build")
        val reported = dao.getJob(job.id)!!
        assertEquals("a self-report must not be VERIFIED", "REPORTED", reported.status)
        assertEquals("I fixed the build", reported.reportedResult)
        assertNull("no verified result without evidence", reported.verifiedResult)

        // Promotion with no supporting artifacts is refused.
        assertFalse(
            "verification with no evidence must be refused",
            projects.verifyWorkerResult(job.id, "build is green", emptyList()),
        )
        assertEquals("REPORTED", dao.getJob(job.id)!!.status)

        // Promotion citing a nonexistent artifact is also refused.
        assertFalse(
            "verification citing a missing artifact must be refused",
            projects.verifyWorkerResult(job.id, "build is green", listOf("art-does-not-exist")),
        )
        assertEquals("REPORTED", dao.getJob(job.id)!!.status)

        // With a real artifact, promotion succeeds.
        val evidence = projects.registerArtifact(
            "test-report.xml", "TEST_REPORT", "file:///t", "0 failures".toByteArray(),
            projectId = project.id, jobId = job.id,
        )
        assertTrue(projects.verifyWorkerResult(job.id, "build is green", listOf(evidence.id)))

        val verified = dao.getJob(job.id)!!
        assertEquals("VERIFIED", verified.status)
        assertEquals("build is green", verified.verifiedResult)
        // The original claim is preserved alongside the verified one.
        assertEquals("I fixed the build", verified.reportedResult)

        db.close()
        file.delete()
    }

    @Test
    fun `structural changes emit Level 0 events`() = runTest {
        val file = dbFile()
        val db = open(file)
        val ledger = RoomConversationLedger(db.conversationEventDao())
        val projects = ProjectRepository(db.projectDao(), ledger)

        val project = projects.createProject("Traceability")
        projects.associateConversation(project.id, "12")
        val job = projects.startJob(project.id, "CODING", "native", "do a thing")
        projects.registerArtifact("a.bin", "OTHER", "file:///a", "x".toByteArray(), project.id, jobId = job.id)
        projects.recordWorkerReport(job.id, "done")

        val events = ledger.queryEventsByTime(0, Long.MAX_VALUE).map { it.rawContent }
        // Operational history is traceable, not just row state.
        assertTrue(events.any { it.startsWith("PROJECT_CREATED") })
        assertTrue(events.any { it.startsWith("PROJECT_ASSOCIATED") })
        assertTrue(events.any { it.startsWith("JOB_DISPATCHED") })
        assertTrue(events.any { it.startsWith("ARTIFACT_REGISTERED") && it.contains("sha256=") })
        assertTrue(events.any { it.startsWith("WORKER_REPORTED_RESULT") })

        db.close()
        file.delete()
    }
}
