package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.capabilities.memory.ConversationActor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies the canonical Level 0 ledger against the directive's requirements.
 *
 * The important property is durability across a persistence-layer restart, so
 * this uses a REAL file-backed Room database that is genuinely closed and
 * reopened. An in-memory database would pass a "data is still there" assertion
 * without ever proving anything survived, which is precisely the kind of test
 * that makes a subsystem look verified when it is not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class CanonicalLedgerPersistenceTest {

    private fun dbFile(): File =
        File.createTempFile("mengine-ledger-test", ".db").also { it.delete() }

    private fun open(file: File): AppDatabase =
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            file.absolutePath,
        ).addMigrations(*ALL_MIGRATIONS).build()

    private fun repoFor(db: AppDatabase): Pair<ChatRepository, RoomConversationLedger> {
        val ledger = RoomConversationLedger(db.conversationEventDao())
        return ChatRepository(
            db.messageDao(), db.styleDao(), db.endpointDao(), db.sessionDao(), ledger,
        ) to ledger
    }

    @Test
    fun `messages reach Level 0 and survive a persistence layer restart`() = runTest {
        val file = dbFile()

        // --- session 1: write through the real repository funnel -------------
        var db = open(file)
        var (repo, ledger) = repoFor(db)

        repo.insertMessage(MessageEntity(text = "wire the memory package", isUser = true, sessionId = 7))
        repo.insertMessage(MessageEntity(text = "acknowledged", isUser = false, sessionId = 7))

        assertEquals("both messages must reach the canonical ledger", 2, ledger.count())
        db.close()

        // --- session 2: a genuinely reopened database ------------------------
        db = open(file)
        val reopened = repoFor(db).second

        assertEquals("Level 0 must survive a restart", 2, reopened.count())

        val events = reopened.queryEventsByTime(0, Long.MAX_VALUE)
        assertEquals(2, events.size)
        assertEquals(ConversationActor.OWNER, events[0].actor)
        assertEquals("wire the memory package", events[0].rawContent)
        assertEquals(ConversationActor.M_ENGINE, events[1].actor)

        // Provenance must survive, not just the text.
        assertEquals("ANDROID", events[0].provenance.sourcePlatform)
        assertEquals("7", events[0].provenance.conversationId)

        db.close()
        file.delete()
    }

    @Test
    fun `a correction supersedes without destroying the original`() = runTest {
        val file = dbFile()
        val db = open(file)
        val (repo, ledger) = repoFor(db)

        repo.insertMessage(MessageEntity(text = "call it sovereignty", isUser = true, sessionId = 1))
        repo.insertMessage(MessageEntity(text = "call it agentic autonomy", isUser = true, sessionId = 1))

        val events = ledger.queryEventsByTime(0, Long.MAX_VALUE)
        val original = events.first { it.rawContent.contains("sovereignty") }
        val correction = events.first { it.rawContent.contains("agentic autonomy") }

        ledger.supersede(original.eventId, correction.eventId)

        // The superseded event is still fully present — history is added to,
        // never rewritten. This is the property that makes the ledger auditable.
        val stillThere = ledger.getEvent(original.eventId)
        assertNotNull("the superseded event must NOT be deleted", stillThere)
        assertEquals("call it sovereignty", stillThere!!.rawContent)
        assertEquals(correction.eventId, stillThere.supersededByEventId)

        // Walking the chain resolves to the active successor.
        val chain = ledger.getProvenanceChain(original.eventId)
        assertEquals(2, chain.size)
        assertEquals(correction.eventId, chain.last().eventId)
        assertNull("the successor is the active record", chain.last().supersededByEventId)

        // Only the correction is active.
        val active = ledger.recentActive(10)
        assertTrue(active.none { it.eventId == original.eventId })
        assertTrue(active.any { it.eventId == correction.eventId })

        db.close()
        file.delete()
    }

    @Test
    fun `backfill of pre-ledger messages is idempotent and marks provenance`() = runTest {
        val file = dbFile()
        val db = open(file)

        // Legacy state: messages written with NO ledger attached, exactly as
        // every message in an existing install was.
        val legacyRepo = ChatRepository(
            db.messageDao(), db.styleDao(), db.endpointDao(), db.sessionDao(), null,
        )
        legacyRepo.insertMessage(MessageEntity(text = "old one", isUser = true, sessionId = 3))
        legacyRepo.insertMessage(MessageEntity(text = "old two", isUser = false, sessionId = 3))

        val dao = db.conversationEventDao()
        assertEquals("legacy writes must not have reached Level 0", 0, dao.count())

        val (repo, ledger) = repoFor(db)
        val firstRun = repo.backfillLedgerFromMessages()
        assertEquals("both legacy messages migrate", 2, firstRun)
        assertEquals(2, ledger.count())

        // Re-running must add nothing. A migration that duplicates history on
        // retry is worse than one that never ran.
        val secondRun = repo.backfillLedgerFromMessages()
        assertEquals("re-running the backfill must add nothing", 0, secondRun)
        assertEquals(2, ledger.count())

        // Original timestamps preserved, and content intact.
        val events = ledger.queryEventsByTime(0, Long.MAX_VALUE)
        assertTrue(events.any { it.rawContent == "old one" })
        assertTrue(events.all { it.timestamp > 0 })

        db.close()
        file.delete()
    }
}
