package dev.juuri.noma.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NoteSort
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

private sealed interface WriteOperation {
    data class Save(val note: Note) : WriteOperation
    data class Delete(val note: Note) : WriteOperation
    data class Restore(val note: Note) : WriteOperation
    data class Archive(val previous: Note) : WriteOperation
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

    private val operations = Channel<WriteOperation>(Channel.UNLIMITED)
    private val eventChannel = Channel<NoteUiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        // One writer preserves keystroke/delete/undo order and prevents stale saves
        // from resurrecting a deleted note.
        viewModelScope.launch {
            for (operation in operations) {
                try {
                    when (operation) {
                        is WriteOperation.Save -> service.save(operation.note)
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
                    }
                } catch (failure: Exception) {
                    eventChannel.send(
                        NoteUiEvent.Failure(failure.localizedMessage ?: "Storage unavailable")
                    )
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
        notes: List<Note>,
        query: String,
        pinnedOnly: Boolean,
        archivedOnly: Boolean,
        sort: NoteSort
    ): List<Note> = service.visible(notes, query, pinnedOnly, archivedOnly, sort)

    fun createNote() {
        if (_creating.value) return
        _archivedOnly.value = false
        _pinnedOnly.value = false
        _creating.value = true
        viewModelScope.launch {
            try {
                _active.value = service.create()
            } catch (failure: Exception) {
                eventChannel.send(
                    NoteUiEvent.Failure(failure.localizedMessage ?: "Cannot create note")
                )
            } finally {
                _creating.value = false
            }
        }
    }

    fun openNote(note: Note) { _active.value = note }
    fun closeNote() { _active.value = null }

    fun changeTitle(value: String) {
        _active.value?.let { updateActive(it.copy(title = value)) }
    }

    fun changeBody(value: String) {
        _active.value?.let { updateActive(it.copy(body = value)) }
    }

    fun togglePin(note: Note) {
        val changed = note.copy(isPinned = !note.isPinned)
        if (_active.value?.id == note.id) _active.value = changed
        operations.trySend(WriteOperation.Save(changed))
    }

    private fun updateActive(note: Note) {
        _active.value = note
        operations.trySend(WriteOperation.Save(note))
    }

    fun deleteNote(note: Note) {
        if (_active.value?.id == note.id) _active.value = null
        operations.trySend(WriteOperation.Delete(note))
    }

    fun undoDelete(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun toggleArchive(note: Note) {
        if (_active.value?.id == note.id) _active.value = null
        operations.trySend(WriteOperation.Archive(note))
    }

    fun undoArchive(note: Note) {
        operations.trySend(WriteOperation.Restore(note))
    }

    fun retry() {
        viewModelScope.launch { repository.refresh() }
    }
}
