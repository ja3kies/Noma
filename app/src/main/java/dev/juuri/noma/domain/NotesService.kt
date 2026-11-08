package dev.juuri.noma.domain

import java.util.Locale
import java.util.UUID

class NotesService(
    private val repository: NotesRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun create(): Note {
        val now = clock()
        return Note(id = idGenerator(), createdAt = now, updatedAt = now).also {
            repository.save(it)
        }
    }

    suspend fun save(note: Note) {
        repository.save(note.copy(updatedAt = clock()))
    }

    suspend fun delete(note: Note) {
        repository.delete(note.id)
    }

    suspend fun restore(note: Note) {
        repository.save(note)
    }

    fun visible(notes: List<Note>, query: String, pinnedOnly: Boolean): List<Note> {
        val term = query.trim().lowercase(Locale.ROOT)
        return notes.asSequence()
            .filter { !pinnedOnly || it.isPinned }
            .filter {
                term.isEmpty() ||
                    it.title.lowercase(Locale.ROOT).contains(term) ||
                    it.body.lowercase(Locale.ROOT).contains(term)
            }
            .sortedWith(compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.updatedAt }
                .thenBy { it.id })
            .toList()
    }
}
