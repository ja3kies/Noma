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
    /** Atomically inserts missing IDs; all-or-nothing on storage failure. */
    suspend fun importMissing(notes: List<Note>): Int
    suspend fun refresh()
}
