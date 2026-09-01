package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies Level 1 owner context hydrates from persistence and that context
 * reconstruction consumes it — i.e. that the memory pipeline is connected end to
 * end rather than merely constructed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class CanonicalMemoryHydrationTest {

    private fun dbFile() = File.createTempFile("mengine-memory-test", ".db").also { it.delete() }

    private fun open(file: File): AppDatabase =
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            file.absolutePath,
        ).addMigrations(*ALL_MIGRATIONS).build()

    private fun memoryFor(db: AppDatabase) =
        CanonicalMemory(RoomConversationLedger(db.conversationEventDao()), db.ownerContextDao())

    @Test
    fun `terminology preference is data, not a Kotlin literal, and reaches reconstruction`() = runTest {
        val file = dbFile()
        val db = open(file)
        val memory = memoryFor(db)

        // Nothing hydrated yet: the graph must be genuinely empty rather than
        // silently carrying a compiled-in default.
        assertTrue(
            "no preference may exist before hydration",
            memory.ownerContext.allTerminologyPreferences().isEmpty(),
        )

        assertTrue("first seed writes", memory.seedDefaultsIfEmpty())
        assertFalse("seeding is once-only", memory.seedDefaultsIfEmpty())

        val count = memory.hydrate()
        assertEquals(1, count)

        // The operating language is in force, loaded from storage.
        assertEquals("Agentic Autonomy", memory.ownerContext.getPreferredTerminology("Sovereignty"))

        // And it actually reaches the reconstructed context handed to a worker.
        val ctx = memory.contextReconstruction.compileTaskContext("ENGINEERING")
        assertEquals(1, ctx.terminologyConstraints.size)
        assertEquals("Agentic Autonomy", ctx.terminologyConstraints.first().preferredTerm)

        db.close()
        file.delete()
    }

    @Test
    fun `a superseded preference stops being applied but is not destroyed`() = runTest {
        val file = dbFile()
        val db = open(file)
        val memory = memoryFor(db)
        val dao = db.ownerContextDao()

        memory.seedDefaultsIfEmpty()
        memory.hydrate()
        assertEquals("Agentic Autonomy", memory.ownerContext.getPreferredTerminology("Sovereignty"))

        // The owner changes their mind.
        memory.supersedePreference(
            oldTerm = "Sovereignty",
            replacement = TerminologyPreferenceEntity(
                rejectedTerm = "Autonomy",
                preferredTerm = "Bounded Autonomy",
                context = "Owner correction",
            ),
        )

        // Active set reflects the correction...
        val active = memory.ownerContext.allTerminologyPreferences()
        assertEquals(1, active.size)
        assertEquals("Bounded Autonomy", active.first().preferredTerm)

        // ...but the superseded row is still on disk with its provenance.
        val all = dao.allPreferences()
        assertEquals("both rows must remain", 2, all.size)
        val old = all.first { it.rejectedTerm == "Sovereignty" }
        assertNotNull("the superseded preference must NOT be deleted", old)
        assertEquals("Autonomy", old.supersededBy)
        assertEquals("Agentic Autonomy", old.preferredTerm)

        db.close()
        file.delete()
    }

    @Test
    fun `reconstruction excludes superseded events and does not dump all history`() = runTest {
        val file = dbFile()
        val db = open(file)
        val memory = memoryFor(db)
        val ledger = memory.ledger
        val repo = ChatRepository(
            db.messageDao(), db.styleDao(), db.endpointDao(), db.sessionDao(), ledger,
        )
        memory.hydrate()

        // Write more history than reconstruction is allowed to replay.
        repeat(10) { i ->
            repo.insertMessage(MessageEntity(text = "event $i", isUser = true, sessionId = 1))
        }
        assertEquals(10, ledger.count())

        val events = ledger.queryEventsByTime(0, Long.MAX_VALUE)
        // Correct the most recent event.
        ledger.supersede(events.last().eventId, events[events.size - 2].eventId)

        val ctx = memory.contextReconstruction.compileTaskContext("ENGINEERING")

        // Level 0 keeps everything; the reconstructed context carries a bounded
        // slice. Dumping the whole ledger into every prompt is the failure this
        // engine exists to prevent.
        assertTrue(
            "reconstruction must not replay the entire ledger",
            ctx.historicalPrecedents.size <= 3,
        )
        assertEquals(10, ledger.count())

        // The corrected event must not be replayed as though it still stood.
        assertTrue(
            "a superseded event must not appear in reconstructed context",
            ctx.historicalPrecedents.none { it.contains("event 9") },
        )

        db.close()
        file.delete()
    }
}
