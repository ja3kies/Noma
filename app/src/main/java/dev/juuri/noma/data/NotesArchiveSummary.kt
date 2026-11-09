package dev.juuri.noma.data

import dev.juuri.noma.domain.Note

/** Read-only report calculated after full JSON schema and UTF-8 validation. */
data class NotesArchiveSummary(
    val total: Int,
    val pinned: Int,
    val archived: Int,
    val mostRecentEdit: Long?,
    val oldestCreation: Long?,
    val totalBodyCharacters: Long
) {
    companion object {
        fun fromNotes(notes: List<Note>): NotesArchiveSummary = NotesArchiveSummary(
            total = notes.size,
            pinned = notes.count { it.isPinned },
            archived = notes.count { it.isArchived },
            mostRecentEdit = notes.maxOfOrNull { it.updatedAt },
            oldestCreation = notes.minOfOrNull { it.createdAt },
            totalBodyCharacters = notes.sumOf { it.body.length.toLong() }
        )

        fun inspect(bytes: ByteArray): NotesArchiveSummary =
            fromNotes(NotesPortableFormat.decode(bytes))
    }
}
