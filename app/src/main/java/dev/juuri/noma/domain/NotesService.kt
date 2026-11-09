package dev.juuri.noma.domain

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

    /** Save a received snippet exactly once, with no transient blank note. */
    suspend fun captureText(title: String, body: String): Note {
        require(body.isNotBlank()) { "Shared text is empty" }
        require(body.length <= 100_000) { "Shared text exceeds limit" }
        require(title.length <= 200) { "Shared title exceeds limit" }
        val now = clock()
        val note = Note(
            id = idGenerator(),
            title = title,
            body = body,
            createdAt = now,
            updatedAt = now
        )
        repository.save(note)
        return note
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
                .thenBy { NoteSearch.normalize(it.displayTitle) }
                .thenByDescending { it.updatedAt }
                .thenBy { it.id }
        }
        val resultOrder = if (words.isEmpty()) comparator else
            compareBy<Note> { NoteSearch.relevance(it, query) }.then(comparator)
        return notes.asSequence()
            .filter { it.isArchived == archivedOnly }
            .filter { !pinnedOnly || it.isPinned }
            .filter { NoteSearch.matches(it, words) }
            .sortedWith(resultOrder)
            .toList()
    }
}
