package dev.juuri.noma.domain

import java.util.Locale

object NoteTags {
    const val MAX_COUNT = 20
    const val MAX_LENGTH = 32

    fun normalize(raw: String): String = raw.trim().removePrefix("#").trim()
        .replace(Regex("\\s+"), " ").take(MAX_LENGTH)

    fun add(note: Note, raw: String): Note {
        val tag = normalize(raw)
        if (tag.isEmpty() || note.tags.any { it.equals(tag, ignoreCase = true) }) return note
        require(note.tags.size < MAX_COUNT) { "Too many tags" }
        return note.copy(tags = note.tags + tag)
    }

    fun remove(note: Note, tag: String): Note =
        note.copy(tags = note.tags.filterNot { it.equals(tag, ignoreCase = true) })

    fun all(notes: List<Note>, archived: Boolean): List<String> =
        notes.asSequence().filter { it.isArchived == archived }
            .flatMap { it.tags.asSequence() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
            .toList()
}
