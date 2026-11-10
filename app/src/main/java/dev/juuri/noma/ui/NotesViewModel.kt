package dev.juuri.noma.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface NoteUiEvent {
    data class Deleted(val note: Note) : NoteUiEvent
    data class ArchiveChanged(val previous: Note) : NoteUiEvent
    data class Imported(val added: Int, val skipped: Int) : NoteUiEvent
    data object SaveBeforeNavigation : NoteUiEvent
    data object QuickCaptureDeferred : NoteUiEvent
    data object QuickCaptureBusy : NoteUiEvent
    data object StorageNotReady : NoteUiEvent
    data object SharedTextEmpty : NoteUiEvent
    data object SharedTextTooLong : NoteUiEvent
    data object SharedTitleTooLong : NoteUiEvent
    data object ShareQueueFull : NoteUiEvent
    data object CaptureInProgress : NoteUiEvent
    data object SharedCaptureSaved : NoteUiEvent
    data object CreatedWhileEditing : NoteUiEvent
    data object SharedCaptureSavedWhileEditing : NoteUiEvent
    data class Failure(val message: String) : NoteUiEvent
}

/** Reports the result of the most recent editor revision, not just a button press. */
sealed interface EditorSaveState {
    data object Saved : EditorSaveState
    data object Saving : EditorSaveState
    data class Failed(val message: String) : EditorSaveState
}

/** Prevents a stale export from appearing complete while the editor has unsaved changes. */
class UnsavedEditorChangesException : IllegalStateException("The current note has unsaved changes")

/** Creation and share capture write outside the serial editor queue. */
class CaptureStillSavingException : IllegalStateException("A note is still being created or captured")

private sealed interface WriteOperation {
    data class Save(val note: Note, val editorRevision: Long? = null) : WriteOperation
    data class Delete(val note: Note) : WriteOperation
    data class Restore(val note: Note) : WriteOperation
    data class Import(
        val notes: List<Note>,
        val result: CompletableDeferred<Int>? = null
    ) : WriteOperation
    data class ExportSnapshot(val result: CompletableDeferred<List<Note>>) : WriteOperation
    data class Archive(val previous: Note) : WriteOperation
}

class NotesViewModel(
    private val repository: NotesRepository,
    private val service: NotesService
) : ViewModel() {
    val snapshot = repository.notes

    private val queuedIncomingShares = ArrayDeque<dev.juuri.noma.capture.CaptureRequest.SharedText>()
    private val _pendingShare = MutableStateFlow<dev.juuri.noma.capture.CaptureRequest.SharedText?>(null)
    val pendingShare: StateFlow<dev.juuri.noma.capture.CaptureRequest.SharedText?> =
        _pendingShare.asStateFlow()
    private val _capturingShare = MutableStateFlow(false)
    val capturingShare: StateFlow<Boolean> = _capturingShare.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    private val _archivedOnly = MutableStateFlow(false)
    val archivedOnly: StateFlow<Boolean> = _archivedOnly.asStateFlow()

    private val _sort = MutableStateFlow(NoteSort.RECENT)
    val sort: StateFlow<NoteSort> = _sort.asStateFlow()

    private val _active = MutableStateFlow<Note?>(null)
    val active: StateFlow<Note?> = _active.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _saveState = MutableStateFlow<EditorSaveState>(EditorSaveState.Saved)
    val saveState: StateFlow<EditorSaveState> = _saveState.asStateFlow()

    private var editorRevision = 0L
    private var quickNoteRequestedAfterSave = false
    private val operations = Channel<WriteOperation>(Channel.UNLIMITED)
    private val eventChannel = Channel<NoteUiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        // Serialize disk writes. A delete cannot be undone by an older queued save.
        // Only the latest editor revision can be marked as saved.
        viewModelScope.launch {
            for (operation in operations) {
                try {
                    when (operation) {
                        is WriteOperation.Save -> {
                            // Ignore superseded queued revisions. The final editor revision
                            // persists the complete buffer, avoiding unnecessary disk writes
                            // and post-discard writes that have not started.
                            if (operation.editorRevision != null &&
                                operation.editorRevision != editorRevision) continue
                            service.save(operation.note)
                            if (operation.editorRevision != null &&
                                operation.editorRevision == editorRevision) {
                                _saveState.value = EditorSaveState.Saved
                                if (quickNoteRequestedAfterSave) {
                                    quickNoteRequestedAfterSave = false
                                    createNote()
                                }
                            }
                        }
                        is WriteOperation.Delete -> {
                            service.delete(operation.note)
                            eventChannel.send(NoteUiEvent.Deleted(operation.note))
                        }
                        is WriteOperation.Restore -> service.restore(operation.note)
                        is WriteOperation.ExportSnapshot -> {
                            // Creation/share capture do not use the editor operations queue.
                            // An export must not silently omit a concurrently created note.
                            if (_creating.value || _capturingShare.value) {
                                throw CaptureStillSavingException()
                            }
                            // Export is ordered behind queued saves. Refuse a stale backup
                            // if the final editor write still has no disk acknowledgement.
                            if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
                                throw UnsavedEditorChangesException()
                            }
                            val current = repository.notes.value as? dev.juuri.noma.domain.NotesSnapshot.Ready
                                ?: error("Notes not ready")
                            operation.result.complete(current.entries.toList())
                        }
                        is WriteOperation.Import -> {
                            val added = repository.importMissing(operation.notes)
                            operation.result?.complete(added)
                            eventChannel.send(NoteUiEvent.Imported(added, operation.notes.size - added))
                        }
                        is WriteOperation.Archive -> {
                            service.save(operation.previous.copy(
                                isArchived = !operation.previous.isArchived
                            ))
                            eventChannel.send(NoteUiEvent.ArchiveChanged(operation.previous))
                        }
                    }
                } catch (failure: Exception) {
                    if (operation is WriteOperation.ExportSnapshot) {
                        operation.result.completeExceptionally(failure)
                    }
                    if (operation is WriteOperation.Import) {
                        operation.result?.completeExceptionally(failure)
                    }
                    val message = failure.localizedMessage ?: "Storage unavailable"
                    val currentEditorWrite = operation is WriteOperation.Save &&
                        operation.editorRevision != null &&
                        operation.editorRevision == editorRevision
                    if (currentEditorWrite) {
                        _saveState.value = EditorSaveState.Failed(message)
                    }
                    // Superseded autosaves can fail while a newer revision is queued.
                    // Only the newest editor failure should interrupt the user.
                    val relevantFailure = operation !is WriteOperation.Save ||
                        operation.editorRevision == null || currentEditorWrite
                    if (relevantFailure && operation !is WriteOperation.ExportSnapshot &&
                        (operation !is WriteOperation.Import || operation.result == null)) {
                        eventChannel.send(NoteUiEvent.Failure(message))
                    }
                }
            }
        }
    }

    fun receiveSharedText(request: dev.juuri.noma.capture.CaptureRequest.SharedText) {
        // Never overwrite an unconfirmed incoming share when another app shares again.
        if (_pendingShare.value == null) {
            _pendingShare.value = request
        } else if (queuedIncomingShares.size < 16) {
            queuedIncomingShares.addLast(request)
        } else {
            eventChannel.trySend(NoteUiEvent.ShareQueueFull)
        }
    }

    private fun showNextShare() {
        _pendingShare.value = if (queuedIncomingShares.isEmpty()) null
            else queuedIncomingShares.removeFirst()
    }

    fun dismissSharedText() {
        if (!_capturingShare.value) showNextShare()
    }

    fun confirmSharedText() {
        val pending = _pendingShare.value ?: return
        if (_capturingShare.value || _creating.value) return
        if (snapshot.value !is dev.juuri.noma.domain.NotesSnapshot.Ready) {
            eventChannel.trySend(NoteUiEvent.StorageNotReady)
            return
        }
        when {
            pending.body.isBlank() -> {
                eventChannel.trySend(NoteUiEvent.SharedTextEmpty)
                return
            }
            pending.body.length > 100_000 -> {
                eventChannel.trySend(NoteUiEvent.SharedTextTooLong)
                return
            }
            pending.title.length > 200 -> {
                eventChannel.trySend(NoteUiEvent.SharedTitleTooLong)
                return
            }
        }
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        val originatingNoteId = _active.value?.id
        val originatingRevision = editorRevision
        _capturingShare.value = true
        viewModelScope.launch {
            try {
                val created = service.captureText(pending.title, pending.body)
                showNextShare()
                clearDiscoveryFilters()
                // A concurrent edit must not be replaced by an incoming share.
                val keepEditing = originatingNoteId != null &&
                    _active.value?.id == originatingNoteId &&
                    (editorRevision != originatingRevision ||
                        _saveState.value != EditorSaveState.Saved)
                if (!keepEditing) {
                    editorRevision++
                    _active.value = created
                    _saveState.value = EditorSaveState.Saved
                }
                eventChannel.send(
                    if (keepEditing) NoteUiEvent.SharedCaptureSavedWhileEditing
                    else NoteUiEvent.SharedCaptureSaved
                )
            } catch (failure: Exception) {
                eventChannel.send(NoteUiEvent.Failure(failure.localizedMessage ?: "Capture failed"))
                // Keep the pending text so the user can retry without asking the sender again.
            } finally {
                _capturingShare.value = false
            }
        }
    }

    fun onQueryChange(value: String) { _query.value = value }
    fun togglePinnedOnly() { _pinnedOnly.value = !_pinnedOnly.value }
    fun toggleArchiveView() {
        _archivedOnly.value = !_archivedOnly.value
        _pinnedOnly.value = false
    }
    fun selectSort(value: NoteSort) { _sort.value = value }
    /** Reset only discovery constraints, leaving note data and the user's sort intact. */
    fun clearDiscoveryFilters() {
        _query.value = ""
        _pinnedOnly.value = false
        _archivedOnly.value = false
    }

    fun visible(
        notes: List<Note>, query: String, pinnedOnly: Boolean,
        archivedOnly: Boolean, sort: NoteSort
    ): List<Note> = service.visible(notes, query, pinnedOnly, archivedOnly, sort)

    /** Launcher/widget capture never discards the active unsaved editor. */
    fun requestQuickNote() {
        if (_creating.value || _capturingShare.value) {
            eventChannel.trySend(NoteUiEvent.QuickCaptureBusy)
            return
        }
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            if (!quickNoteRequestedAfterSave) {
                quickNoteRequestedAfterSave = true
                eventChannel.trySend(NoteUiEvent.QuickCaptureDeferred)
            }
        } else {
            createNote()
        }
    }

    fun createNote() {
        // Do not race a note created from an external share.
        if (_creating.value || _capturingShare.value) return
        if (snapshot.value !is dev.juuri.noma.domain.NotesSnapshot.Ready) {
            eventChannel.trySend(NoteUiEvent.StorageNotReady)
            return
        }
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        val originatingNoteId = _active.value?.id
        val originatingRevision = editorRevision
        _creating.value = true
        viewModelScope.launch {
            try {
                val created = service.create()
                // A failure must preserve the user's current search and filters.
                // Once persisted, show the new note in the unfiltered notebook.
                _query.value = ""
                _archivedOnly.value = false
                _pinnedOnly.value = false
                // A slow creation must not replace a newly edited draft.
                val keepEditing = originatingNoteId != null &&
                    _active.value?.id == originatingNoteId &&
                    (editorRevision != originatingRevision ||
                        _saveState.value != EditorSaveState.Saved)
                if (keepEditing) {
                    eventChannel.send(NoteUiEvent.CreatedWhileEditing)
                } else {
                    editorRevision++
                    _active.value = created
                    _saveState.value = EditorSaveState.Saved
                }
            } catch (failure: Exception) {
                eventChannel.send(
                    NoteUiEvent.Failure(failure.localizedMessage ?: "Cannot create note")
                )
            } finally {
                _creating.value = false
            }
        }
    }

    fun openNote(note: Note) {
        // Otherwise a completed creation/capture could unexpectedly replace
        // the note selected while that operation was in progress.
        if (_creating.value || _capturingShare.value) return
        // Selecting the same card on a tablet must not mark an in-flight save
        // as completed or replace its unsaved editor buffer.
        if (_active.value?.id == note.id) return
        if (_active.value?.id != null && _active.value?.id != note.id &&
            _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        editorRevision++
        _active.value = note
        _saveState.value = EditorSaveState.Saved
    }

    fun cancelDeferredQuickNote() {
        quickNoteRequestedAfterSave = false
    }

    fun closeNote() {
        // Keep navigation stable until external create/capture has completed.
        if (_creating.value || _capturingShare.value) {
            eventChannel.trySend(NoteUiEvent.CaptureInProgress)
            return
        }
        // If the user explicitly discards an unsaved draft, do not leave a
        // queued widget action that could unexpectedly open a note much later.
        quickNoteRequestedAfterSave = false
        editorRevision++
        _active.value = null
        _saveState.value = EditorSaveState.Saved
    }

    fun changeTitle(value: String) {
        _active.value?.let { if (it.title != value) updateActive(it.copy(title = value)) }
    }

    fun changeBody(value: String) {
        _active.value?.let { if (it.body != value) updateActive(it.copy(body = value)) }
    }

    fun togglePin(note: Note) {
        // A stale card or external caller must not bypass the UI's disabled actions.
        if (_creating.value || _capturingShare.value) return
        if (_active.value?.id == note.id &&
            _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        // List cards can lag behind unsaved editor changes on wide screens.
        // Never replace the editor text with a stale list snapshot.
        val current = _active.value?.takeIf { it.id == note.id } ?: note
        if (current.isArchived) return
        val changed = current.copy(isPinned = !current.isPinned)
        if (_active.value?.id == note.id) {
            updateActive(changed)
        } else {
            operations.trySend(WriteOperation.Save(changed))
        }
    }

    private fun updateActive(note: Note) {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        _active.value = updated
        editorRevision++
        _saveState.value = EditorSaveState.Saving
        operations.trySend(WriteOperation.Save(updated, editorRevision))
    }

    fun retrySave() {
        if (_saveState.value !is EditorSaveState.Failed) return
        _active.value?.let(::updateActive)
    }

    fun deleteNote(note: Note) {
        if (_creating.value || _capturingShare.value) return
        val current = _active.value?.takeIf { it.id == note.id } ?: note
        if (_active.value?.id == note.id && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Delete(current))
    }

    fun undoDelete(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun toggleArchive(note: Note) {
        if (_creating.value || _capturingShare.value) return
        val current = _active.value?.takeIf { it.id == note.id } ?: note
        if (_active.value?.id == note.id && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Archive(current))
    }

    fun undoArchive(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun importNotes(notes: List<Note>) { operations.trySend(WriteOperation.Import(notes)) }

    /** Wait for a committed import, so the confirmation can stay open on failure. */
    suspend fun importNotesAwait(notes: List<Note>): Int {
        val result = CompletableDeferred<Int>()
        operations.send(WriteOperation.Import(notes, result))
        return result.await()
    }

    /** Ordered after all queued saves, archive operations and imports. */
    suspend fun snapshotForExport(): List<Note> {
        val deferred = CompletableDeferred<List<Note>>()
        operations.send(WriteOperation.ExportSnapshot(deferred))
        return deferred.await()
    }

    fun retry() {
        viewModelScope.launch { repository.refresh() }
    }
}
