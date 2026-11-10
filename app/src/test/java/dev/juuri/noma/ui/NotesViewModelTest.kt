package dev.juuri.noma.ui

import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.capture.CaptureRequest
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
        private val originalId = initial.id
        private val state = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Ready(listOf(initial)))
        override val notes: StateFlow<NotesSnapshot> = state
        var gate: CompletableDeferred<Unit>? = null
        var newNoteGate: CompletableDeferred<Unit>? = null
        var failForId: String? = null
        var failWrites = false
        var failNextWrites = 0
        var saveCallCount = 0
        var failImports = false
        var importGate: CompletableDeferred<Unit>? = null

        override suspend fun save(note: Note) {
            saveCallCount++
            if (note.id != originalId) newNoteGate?.await()
            gate?.await()
            if (note.id == failForId) throw IllegalStateException("Simulated editor-only failure")
            if (failNextWrites > 0) {
                failNextWrites--
                throw IllegalStateException("Simulated one-time disk failure")
            }
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

    @Test fun discardedFailedDraftCancelsOldDeferredWidgetCapture() = runTest {
        val original = Note("id", "Existing", "Original", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Unsaved edit")
        model.requestQuickNote()
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)

        // The user explicitly chooses Leave unsaved in the editor.
        model.closeNote()
        repo.failWrites = false
        model.openNote(original)
        model.changeTitle("After discard")
        advanceUntilIdle()

        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("id", model.active.value?.id)
        assertEquals("After discard", model.active.value?.title)
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


    @Test fun staleListPinCannotBypassUnsavedEditorProtection() = runTest {
        val original = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Draft not yet on disk")
        model.togglePin(original)
        assertEquals("Draft not yet on disk", model.active.value?.body)
        assertEquals(false, model.active.value?.isPinned)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val persisted = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals("Draft not yet on disk", persisted.body)
        assertEquals(false, persisted.isPinned)
        model.togglePin(original)
        advanceUntilIdle()
        assertEquals(true, model.active.value?.isPinned)
        assertEquals(true, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
    }

    @Test fun archivedNotesCannotBePinnedThroughViewModel() = runTest {
        val archived = Note("id", "Archived", "Body", false, 1L, 1L)
            .copy(isArchived = true)
        val repo = FakeRepository(archived)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(archived)
        model.togglePin(archived)
        advanceUntilIdle()
        assertEquals(false, model.active.value?.isPinned)
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
    }

    @Test fun archivingSelectedNoteUsesItsLatestSavedBody() = runTest {
        val old = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.changeBody("Latest saved body")
        advanceUntilIdle()
        model.toggleArchive(old) // Deliberately stale card snapshot
        advanceUntilIdle()
        val archived = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals(true, archived.isArchived)
        assertEquals("Latest saved body", archived.body)
    }

    @Test fun keepingFailedDraftCancelsDeferredQuickCapture() = runTest {
        val original = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Keep editing")
        model.requestQuickNote()
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        model.cancelDeferredQuickNote()
        repo.failWrites = false
        model.retrySave()
        advanceUntilIdle()
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("id", model.active.value?.id)
        assertEquals("Keep editing", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }


    @Test fun retryFromAlreadySavedEditorDoesNotRewriteTheNote() = runTest {
        val note = Note("id", "Saved", "Content", false, 1L, 1L)
        val repo = FakeRepository(note)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.retrySave()
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals(0, repo.saveCallCount)
        assertEquals(note, (repo.notes.value as NotesSnapshot.Ready).entries.single())
    }

    @Test fun retryDuringPendingSaveDoesNotQueueDuplicateWrite() = runTest {
        val note = Note("id", "Initial", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Unconfirmed text")
        runCurrent()
        model.retrySave()
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.saveCallCount)
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals("Unconfirmed text", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun cancellingDeferredCaptureDuringPendingSaveCreatesNoExtraNote() = runTest {
        val note = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("After")
        model.requestQuickNote()
        model.cancelDeferredQuickNote()
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("After", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
        assertEquals("id", model.active.value?.id)
    }

    @Test fun deletingStaleCardRestoresLatestSavedEditorContentOnUndo() = runTest {
        val old = Note("id", "Title", "Old content", false, 1L, 1L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.changeBody("Latest content")
        advanceUntilIdle()
        model.deleteNote(old)
        advanceUntilIdle()
        val deleted = model.events.first() as NoteUiEvent.Deleted
        assertEquals("Latest content", deleted.note.body)
        assertEquals(0, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        model.undoDelete(deleted.note)
        advanceUntilIdle()
        assertEquals("Latest content", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun archivingStaleCardRestoresLatestSavedEditorContentOnUndo() = runTest {
        val old = Note("id", "Title", "Old content", false, 1L, 1L)
        val repo = FakeRepository(old)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.changeBody("Latest content")
        advanceUntilIdle()
        model.toggleArchive(old)
        advanceUntilIdle()
        val changed = model.events.first() as NoteUiEvent.ArchiveChanged
        assertEquals("Latest content", changed.previous.body)
        assertEquals(true, (repo.notes.value as NotesSnapshot.Ready).entries.single().isArchived)
        model.undoArchive(changed.previous)
        advanceUntilIdle()
        val restored = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals(false, restored.isArchived)
        assertEquals("Latest content", restored.body)
    }

    @Test fun rapidEditsPersistOnlyTheLatestConfirmedRevision() = runTest {
        val old = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.changeTitle("After title edit")
        model.changeBody("After body edit")
        runCurrent()
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        // Obsolete queued revisions are skipped; only the latest complete buffer is written.
        assertEquals(1, repo.saveCallCount)
        val saved = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals("After title edit", saved.title)
        assertEquals("After body edit", saved.body)
    }

    @Test fun searchFiltersAndSortingNeverModifyPersistedNotes() = runTest {
        val original = Note("id", "Original", "Content", true, 1L, 1L)
        val repo = FakeRepository(original)
        val model = NotesViewModel(repo, NotesService(repo))
        model.onQueryChange("something")
        model.togglePinnedOnly()
        model.toggleArchiveView()
        model.selectSort(NoteSort.TITLE)
        model.clearDiscoveryFilters()
        advanceUntilIdle()
        assertEquals(0, repo.saveCallCount)
        assertEquals(original, (repo.notes.value as NotesSnapshot.Ready).entries.single())
        assertEquals(NoteSort.TITLE, model.sort.value)
    }

    @Test fun repeatedCreateTapDuringSlowDiskWritePersistsOneNewNote() = runTest {
        val original = Note("id", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        model.createNote()
        runCurrent()
        assertEquals(true, model.creating.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.saveCallCount)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(false, model.creating.value)
    }

    @Test fun failedEditorSavePreventsSwitchingToDifferentNote() = runTest {
        val original = Note("id", "Original", "", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Keep this draft")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        model.openNote(Note("other", "Other", "", false, 2L, 2L))
        advanceUntilIdle()
        assertEquals("id", model.active.value?.id)
        assertEquals("Keep this draft", model.active.value?.body)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
    }


    @Test fun supersededFailedAutosavesEmitOnlyLatestError() = runTest {
        val original = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        val events = mutableListOf<NoteUiEvent>()
        val observer = launch { model.events.collect { events.add(it) } }
        runCurrent()
        model.openNote(original)
        model.changeTitle("Draft title")
        model.changeBody("Draft body")
        advanceUntilIdle()
        assertEquals(1, events.filterIsInstance<NoteUiEvent.Failure>().size)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals("Draft body", model.active.value?.body)
        observer.cancel()
    }


    @Test fun failedDraftCannotBeSilentlyExportedAsCurrentBackup() = runTest {
        val original = Note("id", "Document", "On disk", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Unsaved private work")
        val task = async { runCatching { model.snapshotForExport() } }
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertTrue(task.await().exceptionOrNull() is UnsavedEditorChangesException)
        assertEquals("On disk", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
        assertEquals("Unsaved private work", model.active.value?.body)
    }

    @Test fun queuedBackupRefusesExportWhenPendingWriteFails() = runTest {
        val original = Note("id", "Document", "Old", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.gate = CompletableDeferred()
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeTitle("Uncommitted title")
        val task = async { runCatching { model.snapshotForExport() } }
        runCurrent()
        assertTrue(!task.isCompleted)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(task.await().exceptionOrNull() is UnsavedEditorChangesException)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
    }

    @Test fun retryAfterFailedSaveAllowsFreshCompleteBackup() = runTest {
        val original = Note("id", "Document", "Old", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Recovered latest text")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        repo.failWrites = false
        model.retrySave()
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        val export = async { model.snapshotForExport() }
        advanceUntilIdle()
        assertEquals("Recovered latest text", export.await().single().body)
    }

    @Test fun deliberateDiscardOfFailedDraftExportsOnlyCommittedText() = runTest {
        val original = Note("id", "Document", "Last committed", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Discarded draft")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        model.closeNote()
        val export = async { model.snapshotForExport() }
        advanceUntilIdle()
        assertEquals("Last committed", export.await().single().body)
        assertEquals(null, model.active.value)
    }

    @Test fun latestSuccessfulWriteCanBeExportedDespiteOlderFailure() = runTest {
        val original = Note("id", "Document", "Old", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failNextWrites = 1
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeTitle("New title")
        runCurrent() // First revision fails before the second is queued.
        model.changeBody("New body")
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        val exported = async { model.snapshotForExport() }
        advanceUntilIdle()
        assertEquals("New title", exported.await().single().title)
        assertEquals("New body", exported.await().single().body)
        assertEquals(2, repo.saveCallCount)
    }

    @Test fun discoveryFiltersCannotBypassUncommittedExportProtection() = runTest {
        val original = Note("id", "Document", "Saved", true, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Failed edit")
        model.onQueryChange("different")
        model.togglePinnedOnly()
        model.toggleArchiveView()
        model.selectSort(NoteSort.TITLE)
        val result = async { runCatching { model.snapshotForExport() } }
        advanceUntilIdle()
        assertTrue(result.await().exceptionOrNull() is UnsavedEditorChangesException)
        assertEquals("Saved", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun blockedExportDoesNotEmitDuplicateGlobalFailure() = runTest {
        val original = Note("id", "Document", "Saved", false, 1L, 1L)
        val repo = FakeRepository(original)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(original)
        model.changeBody("Failed edit")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        // Consume the editor write error so only the export's own UI handles its failure.
        val existing = async { model.events.first() }
        runCurrent()
        assertTrue(existing.await() is NoteUiEvent.Failure)
        val extraEvents = mutableListOf<NoteUiEvent>()
        val observer = launch { model.events.collect { extraEvents.add(it) } }
        runCurrent()
        val blocked = async { runCatching { model.snapshotForExport() } }
        advanceUntilIdle()
        assertTrue(blocked.await().exceptionOrNull() is UnsavedEditorChangesException)
        assertTrue(extraEvents.none { it is NoteUiEvent.Failure })
        observer.cancel()
    }


    @Test fun creatingNoteCannotRaceWithActiveShareCapture() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Shared", "Payload"))
        model.confirmSharedText()
        runCurrent()
        assertTrue(model.capturingShare.value)
        model.createNote()
        assertEquals(false, model.creating.value)
        assertEquals("Payload", model.pendingShare.value?.body)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(false, model.capturingShare.value)
    }

    @Test fun incomingShareWaitsWhileNewNoteIsCreating() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        model.receiveSharedText(CaptureRequest.SharedText("Shared", "Payload"))
        model.confirmSharedText()
        assertTrue(model.creating.value)
        assertEquals(false, model.capturingShare.value)
        assertEquals("Payload", model.pendingShare.value?.body)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("Payload", model.pendingShare.value?.body)
    }

    @Test fun noteSelectionCannotReplacePendingCreation() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        model.openNote(old)
        assertEquals(null, model.active.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.active.value?.id != "old")
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun noteSelectionCannotReplacePendingShareCapture() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Shared", "Payload"))
        model.confirmSharedText()
        runCurrent()
        model.openNote(old)
        assertEquals(null, model.active.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Payload", model.active.value?.body)
    }

    @Test fun exportCannotOmitNoteCurrentlyBeingCreated() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        val blocked = async { runCatching { model.snapshotForExport() } }
        runCurrent()
        assertTrue(blocked.await().exceptionOrNull() is CaptureStillSavingException)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val fresh = async { model.snapshotForExport() }
        advanceUntilIdle()
        assertEquals(2, fresh.await().size)
    }

    @Test fun exportCannotOmitShareStillBeingCaptured() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Shared", "Payload"))
        model.confirmSharedText()
        runCurrent()
        val blocked = async { runCatching { model.snapshotForExport() } }
        runCurrent()
        assertTrue(blocked.await().exceptionOrNull() is CaptureStillSavingException)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val exported = async { model.snapshotForExport() }
        advanceUntilIdle()
        assertEquals("Payload", exported.await().single { it.id != "old" }.body)
    }

    @Test fun pendingCreationCannotInterruptAnActiveSavedNoteUntilFinished() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.createNote()
        runCurrent()
        model.openNote(old)
        assertEquals("old", model.active.value?.id)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.active.value?.id != "old")
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun editingTwoFieldsWhileEarlierWriteBlockedStillSavesFinalBuffer() = runTest {
        val old = Note("old", "Stored", "Prior", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.changeTitle("First title")
        runCurrent()
        model.changeBody("Second body")
        model.changeTitle("Final title")
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        val saved = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals("Final title", saved.title)
        assertEquals("Second body", saved.body)
        assertTrue(repo.saveCallCount <= 2)
    }


    @Test fun unchangedTitleAndBodyNeverTriggerDiskWrites() = runTest {
        val note = Note("id", "Keep title", "Keep body", false, 1L, 1L)
        val repo = FakeRepository(note)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeTitle("Keep title")
        model.changeBody("Keep body")
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals(0, repo.saveCallCount)
        assertEquals(note, (repo.notes.value as NotesSnapshot.Ready).entries.single())
    }

    @Test fun pinCannotOverrideUnconfirmedEditorText() = runTest {
        val note = Note("id", "Original", "Before", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Unsaved draft")
        model.togglePin(note)
        assertEquals("Unsaved draft", model.active.value?.body)
        assertEquals(false, model.active.value?.isPinned)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
        assertEquals("Unsaved draft", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun pinCannotSilentlyResetAFailedAutosave() = runTest {
        val note = Note("id", "Title", "Saved body", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Not yet stored")
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        model.togglePin(note)
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals(false, model.active.value?.isPinned)
        assertEquals("Not yet stored", model.active.value?.body)
        assertEquals("Saved body", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
        repo.failWrites = false
        model.retrySave()
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals("Not yet stored", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun pinSecondTimeDuringFirstWriteCannotInvertLatestState() = runTest {
        val note = Note("id", "Title", "", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.togglePin(note)
        model.togglePin(note)
        assertEquals(true, model.active.value?.isPinned)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(true, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
        assertEquals(1, repo.saveCallCount)
    }

    @Test fun deletingNoteWhileNewCapturePendingCannotDiscardIt() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        model.deleteNote(old)
        assertEquals("Saved", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun archivingNoteWhileNewCapturePendingDoesNotChangeIt() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        model.toggleArchive(old)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.first { it.id == "id" }.isArchived)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun pinWhileNewCapturePendingDoesNotChangeStoredNote() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.createNote()
        runCurrent()
        model.togglePin(old)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.first { it.id == "id" }.isPinned)
        assertEquals(1, repo.saveCallCount)
    }

    @Test fun deleteDuringSharedCaptureLeavesExistingNoteAlone() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("New", "Shared"))
        model.confirmSharedText()
        runCurrent()
        model.deleteNote(old)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("Saved", (repo.notes.value as NotesSnapshot.Ready).entries.first { it.id == "id" }.body)
    }

    @Test fun archiveDuringSharedCaptureLeavesExistingNoteUnchanged() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("New", "Shared"))
        model.confirmSharedText()
        runCurrent()
        model.toggleArchive(old)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.first { it.id == "id" }.isArchived)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun pinDuringSharedCaptureLeavesExistingNoteUnchanged() = runTest {
        val old = Note("id", "Old note", "Saved", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("New", "Shared"))
        model.confirmSharedText()
        runCurrent()
        model.togglePin(old)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.first { it.id == "id" }.isPinned)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }


    @Test fun pinOperationOnSavedEditorPersistsAndRemainsAccessible() = runTest {
        val note = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(note)
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.togglePin(note)
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals(true, model.active.value?.isPinned)
        assertEquals(true, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
    }

    @Test fun pinSaveFailureCanBeRetriedWithoutLosingPinIntent() = runTest {
        val note = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.togglePin(note)
        advanceUntilIdle()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals(true, model.active.value?.isPinned)
        assertEquals(false, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
        repo.failWrites = false
        model.retrySave()
        advanceUntilIdle()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals(true, (repo.notes.value as NotesSnapshot.Ready).entries.single().isPinned)
    }

    @Test fun deleteIsBlockedDuringPendingEditorWrite() = runTest {
        val note = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Pending user text")
        model.deleteNote(note)
        assertEquals("Pending user text", model.active.value?.body)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals("Pending user text", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
    }

    @Test fun archiveIsBlockedDuringPendingEditorWrite() = runTest {
        val note = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Pending text")
        model.toggleArchive(note)
        assertEquals(false, model.active.value?.isArchived)
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val persisted = (repo.notes.value as NotesSnapshot.Ready).entries.single()
        assertEquals(false, persisted.isArchived)
        assertEquals("Pending text", persisted.body)
    }

    @Test fun failedDraftBlocksQuickCaptureWithoutDroppingEditorText() = runTest {
        val note = Note("id", "Original", "Body", false, 1L, 1L)
        val repo = FakeRepository(note)
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(note)
        model.changeBody("Draft")
        advanceUntilIdle()
        model.requestQuickNote()
        assertEquals("id", model.active.value?.id)
        assertEquals("Draft", model.active.value?.body)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }


    @Test fun slowCreationCannotStealCurrentNoteAfterConcurrentSuccessfulEdit() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.createNote()
        runCurrent()
        assertTrue(model.creating.value)
        model.changeBody("Newly edited and saved")
        runCurrent()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("id", model.active.value?.id)
        assertEquals("Newly edited and saved", model.active.value?.body)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(false, model.creating.value)
    }

    @Test fun slowCreationKeepsFailedEditorDraftAndStillCreatesNewNote() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        repo.failForId = "id"
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.createNote()
        runCurrent()
        model.changeBody("Failed edit remains here")
        runCurrent()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("id", model.active.value?.id)
        assertEquals("Failed edit remains here", model.active.value?.body)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        val entries = (repo.notes.value as NotesSnapshot.Ready).entries
        assertEquals(2, entries.size)
        assertEquals("Stored", entries.single { it.id == "id" }.body)
    }

    @Test fun slowCreationLeavesPendingEditorRevisionAndCanFinishSavingLater() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        repo.gate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.createNote()
        runCurrent()
        model.changeBody("Pending edit")
        assertEquals(EditorSaveState.Saving, model.saveState.value)
        repo.newNoteGate!!.complete(Unit)
        runCurrent()
        assertEquals("id", model.active.value?.id)
        assertEquals("Pending edit", model.active.value?.body)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals("Pending edit", model.active.value?.body)
    }

    @Test fun sharedCaptureCannotStealCurrentNoteAfterConcurrentSavedEdit() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.receiveSharedText(CaptureRequest.SharedText("Link", "https://example.org"))
        model.confirmSharedText()
        runCurrent()
        model.changeTitle("Retitled while sharing")
        runCurrent()
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("id", model.active.value?.id)
        assertEquals("Retitled while sharing", model.active.value?.title)
        assertEquals(null, model.pendingShare.value)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun sharedCapturePreservesFailedEditorDraftWithoutOverwritingDisk() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        repo.failForId = "id"
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.receiveSharedText(CaptureRequest.SharedText("Copied", "Message"))
        model.confirmSharedText()
        runCurrent()
        model.changeBody("Failure must be recoverable")
        runCurrent()
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("id", model.active.value?.id)
        assertEquals("Failure must be recoverable", model.active.value?.body)
        assertTrue(model.saveState.value is EditorSaveState.Failed)
        assertEquals("Stored", (repo.notes.value as NotesSnapshot.Ready).entries.single { it.id == "id" }.body)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
    }

    @Test fun sharedCaptureStillOpensNormallyWhenExistingEditorIsUnchanged() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.receiveSharedText(CaptureRequest.SharedText("Copied", "Shared body"))
        model.confirmSharedText()
        runCurrent()
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("Shared body", model.active.value?.body)
        assertEquals(EditorSaveState.Saved, model.saveState.value)
        assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        assertEquals(null, model.pendingShare.value)
    }


    @Test fun slowCreationProvidesFeedbackIfAnotherDraftWasUpdated() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.createNote()
        runCurrent()
        model.changeBody("Kept")
        runCurrent()
        val message = async { model.events.first() }
        runCurrent()
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(NoteUiEvent.CreatedWhileEditing, message.await())
        assertEquals("id", model.active.value?.id)
    }

    @Test fun sharedCaptureProvidesFeedbackIfAnotherDraftWasUpdated() = runTest {
        val old = Note("id", "Original", "Stored", false, 1L, 1L)
        val repo = FakeRepository(old)
        repo.newNoteGate = CompletableDeferred()
        val model = NotesViewModel(repo, NotesService(repo))
        model.openNote(old)
        model.receiveSharedText(CaptureRequest.SharedText("From app", "Imported snippet"))
        model.confirmSharedText()
        runCurrent()
        model.changeBody("Still editing")
        runCurrent()
        val message = async { model.events.first() }
        runCurrent()
        repo.newNoteGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(NoteUiEvent.SharedCaptureSavedWhileEditing, message.await())
        assertEquals("id", model.active.value?.id)
        assertEquals("Still editing", model.active.value?.body)
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
