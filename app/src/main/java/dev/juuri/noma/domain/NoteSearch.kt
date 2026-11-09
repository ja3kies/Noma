package dev.juuri.noma.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Accent-insensitive, case-insensitive search that matches every typed word
 * in either the title or the body. The original note text is never modified.
 */
object NoteSearch {
    private val combiningMarks = Regex("\\p{M}+")

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)

    fun terms(query: String): List<String> =
        normalize(query).trim().split(Regex("\\s+")).filter(String::isNotEmpty)

    fun matches(note: Note, query: String): Boolean =
        matches(note, terms(query))

    fun matches(note: Note, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val title = normalize(note.title)
        // Avoid normalizing or allocating the body for title-only searches.
        if (terms.all(title::contains)) return true
        val body = normalize(note.body)
        return terms.all { title.contains(it) || body.contains(it) }
    }

    /** The nearest body line with a match makes search results recognisable. */
    fun matchingPreview(note: Note, query: String): String {
        val words = terms(query)
        if (words.isEmpty()) return note.preview
        return note.body.lineSequence()
            .map(String::trim)
            .firstOrNull { line -> words.any { normalize(line).contains(it) } }
            ?: note.preview
    }
}
