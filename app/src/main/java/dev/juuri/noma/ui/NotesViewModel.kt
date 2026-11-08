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
    data class Archive(val previous: Note) : WriteOperation
    data class Import(val entries: List<Note>, val completion: CompletableDeferred<Int>) : WriteOperation
    data class Barrier(val completion: CompletableDeferred<Unit>) : WriteOperation
}

class NotesViewModel(
    private val repository: NotesRepository,
    private val service: NotesService
) : ViewModel() {
    val snapshot = repository.notes

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
                            }
                        }
                        is WriteOperation.Delete -> {
                            service.delete(operation.note)
                            eventChannel.send(NoteUiEvent.Deleted(operation.note))
                        }
                        is WriteOperation.Restore -> service.restore(operation.note)
                        is WriteOperation.Archive -> {
                            service.save(operation.previous.copy(
                                isArchived = !operation.previous.isArchived
                            ))
                            eventChannel.send(NoteUiEvent.ArchiveChanged(operation.previous))
                        }
                        is WriteOperation.Import -> {
                            val count = repository.importNotes(operation.entries)
                            operation.completion.complete(count)
                        }
                        is WriteOperation.Barrier -> {
                            repository.refresh()
                            operation.completion.complete(Unit)
                        }
                    }
                } catch (failure: Exception) {
                    when (operation) {
                        is WriteOperation.Import -> operation.completion.completeExceptionally(failure)
                        is WriteOperation.Barrier -> operation.completion.completeExceptionally(failure)
                        else -> Unit
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

    fun onQueryChange(value: String) { _query.value = value }
    fun togglePinnedOnly() { _pinnedOnly.value = !_pinnedOnly.value }
    fun toggleArchiveView() {
        _archivedOnly.value = !_archivedOnly.value
        _pinnedOnly.value = false
    }
    fun selectSort(value: NoteSort) { _sort.value = value }

    fun visible(
        notes: List<Note>, query: String, pinnedOnly: Boolean,
        archivedOnly: Boolean, sort: NoteSort
    ): List<Note> = service.visible(notes, query, pinnedOnly, archivedOnly, sort)

    fun createNote() {
        if (_creating.value) return
        _archivedOnly.value = false
        _pinnedOnly.value = false
        _creating.value = true
        viewModelScope.launch {
            try {
                val created = service.create()
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
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Delete(note))
    }

    fun undoDelete(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun toggleArchive(note: Note) {
        if (_active.value?.id == note.id) closeNote()
        operations.trySend(WriteOperation.Archive(note))
    }

    fun undoArchive(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    /**
     * Suspend until all previously queued edits have committed before exporting.
     * Never silently present an incomplete backup as a successful one.
     */
    suspend fun exportSnapshot(): List<Note> {
        check(_saveState.value !is EditorSaveState.Failed) {
            "Resolve unsaved changes before exporting"
        }
        val barrier = CompletableDeferred<Unit>()
        operations.send(WriteOperation.Barrier(barrier))
        barrier.await()
        val ready = repository.notes.value as? dev.juuri.noma.domain.NotesSnapshot.Ready
            ?: error("Notes unavailable for backup")
        return ready.entries
    }

    suspend fun importNotes(entries: List<Note>): Int {
        if (entries.isEmpty()) return 0
        val result = CompletableDeferred<Int>()
        operations.send(WriteOperation.Import(entries, result))
        return result.await()
    }

    fun retry() {
        viewModelScope.launch { repository.refresh() }
    }
}
