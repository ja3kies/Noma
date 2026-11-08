package dev.juuri.noma.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.transfer.NoteTransferCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TransferPersistenceTest {
    @Test fun batchRestorePreservesExistingRowsAndRemapsCollisions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-transfer-${UUID.randomUUID()}.db"
        try {
            val repo = SqliteNotesRepository(context, name)
            val existing = Note("same", "Unchanged", "Do not overwrite", true, 10, 10)
            repo.save(existing)
            val imported = Note("same", "Recovered", "Backup body", false, 1, 2, true)
            val count = repo.importNotes(listOf(imported))
            assertEquals(1, count)
            val entries = (repo.notes.value as NotesSnapshot.Ready).entries
            assertEquals(2, entries.size)
            assertEquals(existing, entries.single { it.id == "same" })
            val recovered = entries.single { it.title == "Recovered" }
            assertNotEquals("same", recovered.id)
            assertEquals(imported.body, recovered.body)
            assertTrue(recovered.isArchived)
            assertEquals(imported.createdAt, recovered.createdAt)
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun sqliteBackupSurvivesRepositoryRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-roundtrip-${UUID.randomUUID()}.db"
        try {
            val original = Note("uuid", "Title", "Body", false, 1, 5, false)
            SqliteNotesRepository(context, name).save(original)
            val backedUp = NoteTransferCodec.encode(listOf(original))
            val recovered = NoteTransferCodec.decode(backedUp)
            val second = SqliteNotesRepository(context, name)
            val inserted = second.importNotes(recovered)
            assertEquals(1, inserted)
            second.refresh()
            assertEquals(2, (second.notes.value as NotesSnapshot.Ready).entries.size)
        } finally {
            context.deleteDatabase(name)
        }
    }
}
