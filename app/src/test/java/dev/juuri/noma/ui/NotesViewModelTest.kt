package dev.juuri.noma.ui

import dev.juuri.noma.domain.Note
import dev.juuri.noma.capture.CaptureRequest
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
        var failImports = false
        var importGate: CompletableDeferred<Unit>? = null

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
            importGate?.await()
            if (failImports) throw IllegalStateException("Simulated import failure")
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

    @Test fun creatingNoteFromSearchClearsDiscoveryConstraints() = runTest {
        val old = Note("existing", "Existing", "Body", true, 1L, 1L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        model.onQueryChange("impossible phrase")
        model.togglePinnedOnly()
        model.createNote()
        advanceUntilIdle()
        assertEquals("", model.query.value)
        assertEquals(false, model.pinnedOnly.value)
        assertEquals(false, model.archivedOnly.value)
        assertTrue(model.active.value != null)
        assertEquals(EditorSaveState.Saved, model.saveState.value)
    }

    @Test fun creatingNoteFromArchiveReturnsToNormalNotebook() = runTest {
        val old = Note("existing", "Existing", "", false, 1L, 1L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        model.toggleArchiveView()
        model.onQueryChange("old archive search")
        model.createNote()
        advanceUntilIdle()
        assertEquals(false, model.archivedOnly.value)
        assertEquals("", model.query.value)
        assertTrue(model.active.value != null)
    }

    @Test fun failedQuickCapturePreservesCurrentSearchAndFilters() = runTest {
        val old = Note("existing", "Keep me", "", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.onQueryChange("keep")
        model.togglePinnedOnly()
        model.createNote()
        advanceUntilIdle()
        assertEquals("keep", model.query.value)
        assertEquals(true, model.pinnedOnly.value)
        assertEquals(null, model.active.value)
        assertEquals(false, model.creating.value)
    }

    @Test fun switchingNotesDuringPendingSaveKeepsDraftAndSavingState() = runTest {
        val original = Note("current", "Initial", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Uncommitted changes")
        model.openNote(Note("other", "Different note", "", false, 2L, 2L))
        assertEquals("current", model.active.value?.id)
        assertEquals("Uncommitted changes", model.active.value?.body)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        model.openNote(original)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
    }

    @Test fun failedSaveCannotBeLostThroughDeleteOrArchive() = runTest {
        val original = Note("current", "Unchanged", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Draft still in memory")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        model.deleteNote(original)
        model.toggleArchive(original)
        model.createNote()
        assertEquals("current", model.active.value?.id)
        assertEquals("Draft still in memory", model.active.value?.body)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun receivingSharedTextDoesNotWriteUntilExplicitConfirmation() = runTest {
        val original = Note("saved", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("From browser", "https://example.org"))
        advanceUntilIdle()
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("https://example.org", model.pendingShare.value?.body)
        model.dismissSharedText()
        assertEquals(null, model.pendingShare.value)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun confirmingSharedTextPersistsAndOpensExactlyOneNote() = runTest {
        val original = Note("saved", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        val model = NotesViewModel(repo, NotesService(repo))
        val pending = CaptureRequest.SharedText("Shared thought", "An idea from another app")
        model.receiveSharedText(pending)
        model.confirmSharedText()
        model.confirmSharedText() // Duplicate tap cannot create a second note.
        advanceUntilIdle()
        assertEquals(null, model.pendingShare.value)
        assertEquals(false, model.capturingShare.value)
        val notes = (repo.notes.value as NotesSnapshot.Ready).entries
        assertEquals(2, notes.size)
        assertEquals(1, notes.count { it.body == pending.body })
        assertEquals("Shared thought", model.active.value?.title)
    }

    @Test fun failedSharedCaptureRetainsContentsForRetry() = runTest {
        val original = Note("saved", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("", "Do not lose this"))
        model.confirmSharedText()
        advanceUntilIdle()
        assertEquals("Do not lose this", model.pendingShare.value?.body)
        assertEquals(false, model.capturingShare.value)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        repo.failWrites = false
        model.confirmSharedText()
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(null, model.pendingShare.value)
    }

    @Test fun pendingEditorSavePreventsSharedCaptureFromReplacingDraft() = runTest {
        val original = Note("saved", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Uncommitted")
        model.receiveSharedText(CaptureRequest.SharedText("", "Incoming message"))
        model.confirmSharedText()
        assertEquals("Uncommitted", model.active.value?.body)
        assertEquals("Incoming message", model.pendingShare.value?.body)
        assertEquals(false, model.capturingShare.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        model.confirmSharedText()
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun multipleIncomingSharesArePresentedInOrderWithoutOverwriting() = runTest {
        val existing = Note("old", "Old", "", false, 1L, 1L)
        val repo = FakeRepository(existing)
        val model = NotesViewModel(repo, NotesService(repo))
        val first = CaptureRequest.SharedText("First", "Content one")
        val second = CaptureRequest.SharedText("Second", "Content two")
        val third = CaptureRequest.SharedText("Third", "Content three")
        model.receiveSharedText(first)
        model.receiveSharedText(second)
        model.receiveSharedText(third)
        assertEquals(first, model.pendingShare.value)
        model.dismissSharedText()
        assertEquals(second, model.pendingShare.value)
        model.confirmSharedText()
        advanceUntilIdle()
        assertEquals(third, model.pendingShare.value)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        model.confirmSharedText()
        advanceUntilIdle()
        assertEquals(null, model.pendingShare.value)
        assertEquals(3, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun sharesStayQueuedWhileAWriteIsUnconfirmed() = runTest {
        val existing = Note("old", "Old", "", false, 1L, 1L)
        val repo = FakeRepository(existing)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("", "First pending"))
        model.confirmSharedText()
        runCurrent()
        model.receiveSharedText(CaptureRequest.SharedText("", "Second pending"))
        model.dismissSharedText()
        assertEquals("First pending", model.pendingShare.value?.body)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Second pending", model.pendingShare.value?.body)
        model.dismissSharedText()
        assertEquals(null, model.pendingShare.value)
    }

    @Test fun widgetQuickNoteWaitsForUnsavedEditorAndPreservesTheDraft() = runTest {
        val original = Note("id", "Existing", "Original", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Latest unsaved edit")
        model.requestQuickNote()
        runCurrent()
        assertEquals("id", model.active.value?.id)
        assertEquals("Latest unsaved edit", model.active.value?.body)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val entries = (repo.notes.value as NotesSnapshot.Ready).entries
        assertEquals(2, entries.size)
        assertEquals("Latest unsaved edit", entries.first { it.id == "id" }.body)
        assertTrue(model.active.value?.id != "id")
    }

    @Test fun repeatedWidgetTapDuringPendingSaveCreatesJustOneNewNote() = runTest {
        val original = Note("id", "Old", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeTitle("New title")
        model.requestQuickNote()
        model.requestQuickNote()
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun failedSaveDoesNotLaunchDeferredWidgetCapture() = runTest {
        val original = Note("id", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Unsaved text")
        model.requestQuickNote()
        advanceUntilIdle()
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("Unsaved text", model.active.value?.body)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        repo.failWrites = false
        model.retrySave()
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun acknowledgedImportWaitsForRepositoryCommit() = runTest {
        val old = Note("old", "Existing", "", false, 1L, 1L)
        val incoming = Note("new", "Imported", "Body", false, 2L, 3L)
        val repo = FakeRepository(old)
        repo.importGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        val task = async { model.importNotesAwait(listOf(incoming)) }
        runCurrent()
        assertTrue(!task.isCompleted)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        repo.importGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, task.await())
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun failedImportReportsFailureWithoutInsertingAndCanRetry() = runTest {
        val old = Note("old", "Original", "", false, 1L, 1L)
        val incoming = Note("new", "Restored", "Content", false, 4L, 5L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        repo.failImports = true
        // Capture the expected failure *inside* the child task, otherwise
        // runTest also reports the async child as an uncaught failure.
        val failed = async { runCatching { model.importNotesAwait(listOf(incoming)) } }
        advanceUntilIdle()
        val outcome = failed.await()
        assertTrue(outcome.isFailure)
        assertTrue(outcome.exceptionOrNull() is IllegalStateException)
        assertEquals(listOf(old), (repo.notes.value as NotesSnapshot.Ready).entries)
        repo.failImports = false
        val retry = async { model.importNotesAwait(listOf(incoming)) }
        advanceUntilIdle()
        assertEquals(1, retry.await())
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
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
