package dev.juuri.noma.domain

import kotlinx.coroutines.flow.StateFlow

sealed interface NotesSnapshot {
    data object Loading : NotesSnapshot
    data class Ready(val entries: List<Note>) : NotesSnapshot
    data class Error(val message: String) : NotesSnapshot
}

interface NotesRepository {
    val notes: StateFlow<NotesSnapshot>
    suspend fun save(note: Note)
    suspend fun delete(id: String)
    suspend fun refresh()

    /** Import without overwriting existing notes. Implementations should commit atomically. */
    suspend fun importNotes(entries: List<Note>): Int {
        entries.forEach { note ->
            save(note.copy(id = java.util.UUID.randomUUID().toString()))
        }
        return entries.size
    }
}
