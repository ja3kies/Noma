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

    @Test fun importingSkipsExistingWithoutOverwritingAndPreservesFields() = runBlocking {
        val repository = SqliteNotesRepository(context, testDb)
        val original = Note("shared", "Original", "Keep original body", true, 10L, 20L, true)
        val fresh = Note("fresh", "Import", "Unicode 🦊", false, 30L, 40L, false)
        repository.save(original)
        val imported = repository.importMissing(listOf(original.copy(title = "Overwritten?"), fresh))
        assertEquals(1, imported)
        val all = (repository.notes.value as NotesSnapshot.Ready).entries.associateBy { it.id }
        assertEquals(2, all.size)
        assertEquals(original, all["shared"])
        assertEquals(fresh, all["fresh"])
        assertEquals(0, repository.importMissing(listOf(original, fresh)))
    }

    @Test fun databaseRejectsImpossibleNoteAndRollsBackPriorInsert() = runBlocking {
        val repository = SqliteNotesRepository(context, testDb)
        val allowed = Note("valid", "Should be rolled back", "", false, 1L, 2L)
        val bad = allowed.copy(id = "bad", title = "x".repeat(3000))
        // Verify rollback via a SQL-triggered failure rather than encoding restrictions.
        NotesOpenHelper(context, testDb).writableDatabase.execSQL(
            "CREATE TRIGGER fail_bad BEFORE INSERT ON notes " +
                "WHEN NEW.id = 'bad' BEGIN SELECT RAISE(ABORT, 'forced import failure'); END"
        )
        try {
            repository.importMissing(listOf(allowed, bad))
            fail("Expected transactional failure")
        } catch (_: android.database.sqlite.SQLiteException) {
            val current = (repository.notes.value as? NotesSnapshot.Ready)?.entries.orEmpty()
            assertTrue(current.none { it.id == "valid" || it.id == "bad" })
            val reopened = NotesOpenHelper(context, testDb).readAll()
            assertTrue(reopened.isEmpty())
        }
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
