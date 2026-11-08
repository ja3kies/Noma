package dev.juuri.noma.ui

import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.CompletableDeferred
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
