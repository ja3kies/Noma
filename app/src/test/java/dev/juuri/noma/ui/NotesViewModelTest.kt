package dev.juuri.noma.ui

import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class FakeRepository(initial: Note) : NotesRepository {
        private val state = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Ready(listOf(initial)))
        override val notes: StateFlow<NotesSnapshot> = state
        var gate: CompletableDeferred<Unit>? = null
        var failWrites = false

        override suspend fun save(note: Note) {
            gate?.await()
            if (failWrites) throw IllegalStateException("Simulated disk failure")
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == note.id } + note)
        }
        override suspend fun delete(id: String) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == id })
        }
        override suspend fun importMissing(notes: List<Note>): Int {
            val old = (state.value as NotesSnapshot.Ready).entries
            val ids = old.mapTo(HashSet<String>()) { it.id }
            val missing = notes.filter { ids.add(it.id) }
            state.value = NotesSnapshot.Ready(old + missing)
            return missing.size
        }
        override suspend fun refresh() = Unit
    }

    @Test fun savedStatusFollowsLatestWriteAcknowledgement() = runTest {
        val note = Note("id", "Initial", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeTitle("Updated")
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        runCurrent()
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals("Updated", (repo.notes.value as NotesSnapshot.Ready).entries.single().title)
    }

    @Test fun exportSnapshotWaitsForLatestCommittedEditorWrite() = runTest {
        val note = Note("id", "Before", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeTitle("After")
        val result = async { model.snapshotForExport() }
        runCurrent()
        assertTrue(!result.isCompleted)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("After", result.await().single().title)
        assertEquals(EditorSaveState.Saved, model.saveState.value)
    }

    @Test fun importsNeverReplaceExistingIds() = runTest {
        val note = Note("id", "Existing", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        val model = NotesViewModel(repo, NotesService(repo))
        model.importNotes(listOf(note.copy(title = "Injected"),
            Note("new", "Imported", "Body", false, 2L, 3L)))
        advanceUntilIdle()
        val entries = (repo.notes.value as NotesSnapshot.Ready).entries.associateBy { it.id }
        assertEquals(2, entries.size)
        assertEquals("Existing", entries["id"]?.title)
        assertEquals("Imported", entries["new"]?.title)
    }

    @Test fun diskFailureNeverShowsSaved() = runTest {
        val note = Note("id", "Initial", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Unsaved content")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals("", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
