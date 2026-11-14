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
        // Imported notes can carry timestamps from a device with a fast clock.
        // A local edit must not make updatedAt precede the creation time.
        repository.save(note.copy(updatedAt = maxOf(clock(), note.createdAt, note.updatedAt)))
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
        // Filtering and sorting an empty notebook should allocate nothing.
        if (notes.isEmpty()) return emptyList()
        val words = NoteSearch.terms(query)
        val normalizedPhrase = if (words.isEmpty()) "" else words.joinToString(" ")
        // Compute expensive Unicode normalization and relevance only once per
        // matching note, not on every comparison performed by the sorter.
        data class SortEntry(val note: Note, val relevance: Int, val titleKey: String)
        val pinnedFirst = compareByDescending<SortEntry> {
            !archivedOnly && it.note.isPinned
        }
        val comparator = when (sort) {
            NoteSort.RECENT -> pinnedFirst
                .thenByDescending { it.note.updatedAt }.thenBy { it.note.id }
            NoteSort.TITLE -> pinnedFirst
                .thenBy { it.titleKey }
                .thenByDescending { it.note.updatedAt }.thenBy { it.note.id }
        }
        val resultOrder = if (words.isEmpty()) comparator else
            compareBy<SortEntry> { it.relevance }.then(comparator)
        return notes.asSequence()
            .filter { it.isArchived == archivedOnly }
            .filter { !pinnedOnly || it.isPinned }
            .filter { NoteSearch.matches(it, words) }
            .map { note ->
                SortEntry(
                    note = note,
                    relevance = if (words.isEmpty()) 0 else NoteSearch.relevanceForTerms(note, words, normalizedPhrase),
                    titleKey = if (sort == NoteSort.TITLE) NoteSearch.normalize(note.displayTitle)
                        else ""
                )
            }
            .sortedWith(resultOrder)
            .map { it.note }
            .toList()
    }
}
