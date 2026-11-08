package dev.juuri.noma.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqlitePersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    // This database is dedicated to instrumentation; never touch real user notes.
    private val testDb = "noma-instrumentation-only.db"

    @Before fun reset() {
        context.deleteDatabase(testDb)
    }

    @After fun cleanup() {
        context.deleteDatabase(testDb)
    }

    @Test fun upgradingV1KeepsUserContentAndPins() {
        context.openOrCreateDatabase(testDb, 0, null).use { db ->
            db.execSQL(
                "CREATE TABLE notes (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, " +
                    "body TEXT NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO notes (id, title, body, pinned, created_at, updated_at) " +
                    "VALUES ('legacy', 'Original', 'Keep this text', 1, 100, 200)"
            )
            db.version = 1
        }
        val migrated = NotesOpenHelper(context, testDb).readAll().single()
        assertEquals("Original", migrated.title)
        assertEquals("Keep this text", migrated.body)
        assertTrue(migrated.isPinned)
        assertFalse(migrated.isArchived)
    }

    @Test fun writesSurviveNewRepositoryInstancesAndDeletion() = runBlocking {
        val first = SqliteNotesRepository(context, testDb)
        val original = Note("persisted", "Shopping", "Coffee and bread", true, 10L, 10L, true)
        first.save(original)

        // A second repository represents an app re-open with a fresh SQLite helper.
        val second = SqliteNotesRepository(context, testDb)
        second.refresh()
        val restored = (second.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals(original, restored)

        second.delete(original.id)
        val third = SqliteNotesRepository(context, testDb)
        third.refresh()
        assertTrue((third.notes.value as NotesSnapshot.Ready).entries.isEmpty())
    }
}
