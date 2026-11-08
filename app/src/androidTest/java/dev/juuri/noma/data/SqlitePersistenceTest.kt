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

    @Before fun reset() {
        context.deleteDatabase(NotesOpenHelper.DATABASE_NAME)
    }

    @After fun cleanup() {
        context.deleteDatabase(NotesOpenHelper.DATABASE_NAME)
    }

    @Test fun writesSurviveNewRepositoryInstancesAndDeletion() = runBlocking {
        val first = SqliteNotesRepository(context)
        val original = Note("persisted", "Shopping", "Coffee and bread", true, 10L, 10L)
        first.save(original)

        // A second repository represents an app re-open with a fresh SQLite helper.
        val second = SqliteNotesRepository(context)
        second.refresh()
        val restored = (second.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals(original, restored)

        second.delete(original.id)
        val third = SqliteNotesRepository(context)
        third.refresh()
        assertTrue((third.notes.value as NotesSnapshot.Ready).entries.isEmpty())
    }
}
