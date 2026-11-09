package dev.juuri.noma.domain

import java.util.Locale
import java.util.UUID

enum class NoteSort { RECENT, TITLE }

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

    fun visible(
        notes: List<Note>,
        query: String,
        pinnedOnly: Boolean,
        archivedOnly: Boolean = false,
        sort: NoteSort = NoteSort.RECENT
    ): List<Note> {
        val words = NoteSearch.terms(query)
        val pinnedFirst = compareByDescending<Note> { !archivedOnly && it.isPinned }
        val comparator = when (sort) {
            NoteSort.RECENT -> pinnedFirst.thenByDescending { it.updatedAt }.thenBy { it.id }
            NoteSort.TITLE -> pinnedFirst
                .thenBy { it.displayTitle.lowercase(Locale.ROOT) }
                .thenByDescending { it.updatedAt }
                .thenBy { it.id }
        }
        return notes.asSequence()
            .filter { it.isArchived == archivedOnly }
            .filter { !pinnedOnly || it.isPinned }
            .filter { NoteSearch.matches(it, words) }
            .sortedWith(comparator)
            .toList()
    }
}
