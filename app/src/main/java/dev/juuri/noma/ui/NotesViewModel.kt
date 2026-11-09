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
    data object SharedCaptureSaved : NoteUiEvent
    data class Failure(val message: String) : NoteUiEvent
}

/** Reports the result of the most recent editor revision, not just a button press. */
sealed interface EditorSaveState {
    data object Saved : EditorSaveState
    data object Saving : EditorSaveState
    data class Failed(val message: String) : EditorSaveState
}

private sealed interface WriteOperation {
    data class Save(val note: Note, val editorRevision: Long? = null) : WriteOperation
    data class Delete(val note: Note) : WriteOperation
    data class Restore(val note: Note) : WriteOperation
    data class Import(val notes: List<Note>) : WriteOperation
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
                            val current = repository.notes.value as? dev.juuri.noma.domain.NotesSnapshot.Ready
                                ?: error("Notes not ready")
                            operation.result.complete(current.entries.toList())
                        }
                        is WriteOperation.Import -> {
                            val added = repository.importMissing(operation.notes)
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
                    val message = failure.localizedMessage ?: "Storage unavailable"
                    if (operation is WriteOperation.Save &&
                        operation.editorRevision != null &&
                        operation.editorRevision == editorRevision) {
                        _saveState.value = EditorSaveState.Failed(message)
                    }
                    eventChannel.send(NoteUiEvent.Failure(message))
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
            eventChannel.trySend(NoteUiEvent.Failure("Too many incoming notes; finish current captures first"))
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
        if (_capturingShare.value) return
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        _capturingShare.value = true
        viewModelScope.launch {
            try {
                val created = service.captureText(pending.title, pending.body)
                showNextShare()
                clearDiscoveryFilters()
                editorRevision++
                _active.value = created
                _saveState.value = EditorSaveState.Saved
                eventChannel.send(NoteUiEvent.SharedCaptureSaved)
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
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            quickNoteRequestedAfterSave = true
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
        } else {
            createNote()
        }
    }

    fun createNote() {
        if (_creating.value) return
        if (_active.value != null && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        _creating.value = true
        viewModelScope.launch {
            try {
                val created = service.create()
                // A failure must preserve the user's current search and filters.
                // Once persisted, show the new note in the unfiltered notebook.
                _query.value = ""
                _archivedOnly.value = false
                _pinnedOnly.value = false
                editorRevision++
                _active.value = created
                _saveState.value = EditorSaveState.Saved
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

    fun closeNote() {
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
        val changed = note.copy(isPinned = !note.isPinned)
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
        _active.value?.let(::updateActive)
    }

    fun deleteNote(note: Note) {
        if (_active.value?.id == note.id && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Delete(note))
    }

    fun undoDelete(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun toggleArchive(note: Note) {
        if (_active.value?.id == note.id && _saveState.value != EditorSaveState.Saved) {
            eventChannel.trySend(NoteUiEvent.SaveBeforeNavigation)
            return
        }
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Archive(note))
    }

    fun undoArchive(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun importNotes(notes: List<Note>) { operations.trySend(WriteOperation.Import(notes)) }

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
