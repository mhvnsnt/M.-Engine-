package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.capabilities.ecology.RemoteControlPlaneRepository
import com.example.ai.capabilities.memory.ConversationActor
import com.example.ai.capabilities.memory.ConversationEvent
import com.example.ai.capabilities.memory.EventProvenance
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the canonical-sync wiring.
 *
 * The sync shipped unreachable: the ledger built its OWN
 * `RemoteControlPlaneRepository`, connection state is per instance, and only
 * `refreshState()` — never called on that private copy — sets CONNECTED. Both
 * sync calls were gated on CONNECTED and both call sites swallowed the result,
 * so a sync that could never run looked exactly like one that worked.
 *
 * These tests pin the three properties that made it invisible: one shared
 * authority, an outcome you can read, and a pull cursor that does not race the
 * local device against itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class CanonicalSyncWiringTest {

    private fun db(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()

    private fun event(id: String, ts: Long, source: String = "ANDROID") = ConversationEvent(
        eventId = id,
        timestamp = ts,
        actor = ConversationActor.OWNER,
        rawContent = "content-$id",
        provenance = EventProvenance(
            sourcePlatform = source,
            conversationId = "conv-1",
            referencedArtifacts = emptyList(),
        ),
        supersededByEventId = null,
    )

    @Test
    fun `there is exactly one control plane authority`() {
        // Four independent instances is what made ledger sync dead code: the
        // one the UI connects is not the one the ledger asks.
        assertSame(RemoteControlPlaneRepository.shared, RemoteControlPlaneRepository.shared)
    }

    @Test
    fun `a sync that cannot run reports why instead of staying silent`() = runTest {
        val database = db()
        val ledger = RoomConversationLedger(database.conversationEventDao())

        // Not connected: the push must not merely do nothing, it must say so.
        ledger.pushToCanonical(event("e1", 1_000L))

        val diag = ledger.syncDiagnostic.value
        assertEquals(LedgerSyncOutcome.NOT_CONNECTED, diag.lastPush)
        assertEquals(0L, diag.pushedEvents)
        assertNotEquals(LedgerSyncOutcome.SYNCED, diag.lastPush)
        database.close()
    }

    @Test
    fun `a failed push never costs the canonical local write`() = runTest {
        val database = db()
        val ledger = RoomConversationLedger(database.conversationEventDao())

        // The control plane is unreachable here. Level 0 is local and must not
        // be gated on it.
        ledger.appendSuspending(event("local-1", 5_000L))

        assertEquals(1, ledger.count())
        assertEquals("content-local-1", ledger.getEvent("local-1")?.rawContent)
        database.close()
    }

    @Test
    fun `the pull cursor tracks synced events, not the newest local message`() = runTest {
        val database = db()
        val dao = database.conversationEventDao()
        val ledger = RoomConversationLedger(dao)

        // An event that arrived from the control plane, then a much newer
        // locally authored one.
        dao.append(
            ConversationEventEntity(
                eventId = "remote-1",
                timestamp = 1_000L,
                actor = "OWNER",
                rawContent = "from control plane",
                sourcePlatform = "WEB",
                conversationId = "conv-1",
                referencedArtifacts = "",
                supersededByEventId = null,
                migratedFrom = SYNC_ORIGIN,
            ),
        )
        ledger.appendSuspending(event("local-1", 9_000L))

        // The cursor must stay at the synced event.
        assertEquals(1_000L, dao.latestSyncedTimestamp(SYNC_ORIGIN))

        // And it must genuinely DIFFER from the newest-event-overall watermark
        // the first version used. Without this contrast the assertion above
        // would still pass if someone reverted to `recentActive(1)` on a
        // fixture where the two happen to coincide.
        val newestOverall = dao.recentActive(1).first().timestamp
        assertEquals(9_000L, newestOverall)
        assertNotEquals(
            "the pull cursor must not be the newest local event",
            newestOverall,
            dao.latestSyncedTimestamp(SYNC_ORIGIN),
        )
        database.close()
    }

    @Test
    fun `the cursor is null before anything has ever synced`() = runTest {
        val database = db()
        val dao = database.conversationEventDao()
        RoomConversationLedger(dao).appendSuspending(event("local-1", 7_000L))

        // Null becomes a since=0 full pull, which append-IGNORE makes idempotent.
        assertNull(dao.latestSyncedTimestamp(SYNC_ORIGIN))
        database.close()
    }

    @Test
    fun `re-pulling the same events cannot duplicate history`() = runTest {
        val database = db()
        val dao = database.conversationEventDao()
        val row = ConversationEventEntity(
            eventId = "remote-1",
            timestamp = 1_000L,
            actor = "OWNER",
            rawContent = "from control plane",
            sourcePlatform = "WEB",
            conversationId = "conv-1",
            referencedArtifacts = "",
            supersededByEventId = null,
            migratedFrom = SYNC_ORIGIN,
        )
        dao.appendAll(listOf(row))
        val second = dao.appendAll(listOf(row))

        assertEquals(1, dao.count())
        assertTrue("a conflicting insert must report -1", second.all { it == -1L })
        database.close()
    }
}
