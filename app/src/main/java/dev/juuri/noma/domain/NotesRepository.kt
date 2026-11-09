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
    /** Returns imported count. Implementations must not replace existing IDs. */
    suspend fun importMissing(notes: List<Note>): Int {
        val existing = (this.notes.value as? NotesSnapshot.Ready)?.entries?.mapTo(HashSet()) { it.id }
            ?: error("Notes are not loaded")
        var count = 0
        for (note in notes) {
            if (existing.add(note.id)) { save(note); count++ }
        }
        return count
    }
    suspend fun refresh()
}
