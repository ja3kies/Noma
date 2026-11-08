package dev.juuri.noma.domain

import java.util.Locale
import java.util.UUID

enum class NoteSort { RECENT, TITLE }

class NotesService(
    private val repository: NotesRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun create(title: String = "", body: String = "", tags: List<String> = emptyList()): Note {
        val now = clock()
        return Note(
            id = idGenerator(), title = title, body = body, tags = tags,
            createdAt = now, updatedAt = now
        ).also {
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
        sort: NoteSort = NoteSort.RECENT,
        tag: String? = null
    ): List<Note> {
        val term = query.trim().lowercase(Locale.ROOT)
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
            .filter { tag == null || it.tags.any { value -> value.equals(tag, true) } }
            .filter {
                term.isEmpty() ||
                    it.title.lowercase(Locale.ROOT).contains(term) ||
                    it.body.lowercase(Locale.ROOT).contains(term) ||
                    it.tags.any { value -> value.lowercase(Locale.ROOT).contains(term) }
            }
            .sortedWith(comparator)
            .toList()
    }
}
