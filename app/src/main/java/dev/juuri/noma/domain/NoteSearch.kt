package dev.juuri.noma.domain

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class SearchScope { ACTIVE, ARCHIVED, ALL }

class ParsedNoteQuery(
    val scope: SearchScope?,
    private val predicates: List<(Note) -> Boolean>
) {
    fun matches(note: Note): Boolean = predicates.all { it(note) }
}

/**
 * Bounded, local-only query language. No regex evaluation from user input.
 * Unrecognized operators are handled as literal text, not silently ignored.
 */
object NoteSearch {
    const val MAX_QUERY_LENGTH = 512
    const val MAX_CLAUSES = 32

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): ParsedNoteQuery {
        var scope: SearchScope? = null
        val rules = mutableListOf<(Note) -> Boolean>()
        for (token in tokenize(text).take(MAX_CLAUSES)) {
            val excluded = token.startsWith("-") && token.length > 1
            val word = if (excluded) token.drop(1) else token
            val lower = word.lowercase(Locale.ROOT)
            if (!excluded && lower in listOf("in:archive", "is:archived")) {
                scope = SearchScope.ARCHIVED
                continue
            }
            if (!excluded && lower in listOf("in:active", "is:active")) {
                scope = SearchScope.ACTIVE
                continue
            }
            if (!excluded && lower == "in:all") {
                scope = SearchScope.ALL
                continue
            }
            val rule: (Note) -> Boolean = when {
                lower == "is:pinned" -> { note -> note.isPinned }
                lower == "is:unpinned" -> { note -> !note.isPinned }
                lower == "has:checklist" -> { note -> ChecklistSyntax.items(note.body).isNotEmpty() }
                lower == "has:tags" -> { note -> note.tags.isNotEmpty() }
                lower.startsWith("#") && word.length > 1 -> {
                    val wanted = word.drop(1)
                    { note: Note -> note.tags.any { it.equals(wanted, true) } }
                }
                lower.startsWith("tag:") && word.length > 4 -> {
                    val wanted = word.drop(4)
                    { note: Note -> note.tags.any { it.equals(wanted, true) } }
                }
                lower.startsWith("title:") && word.length > 6 -> {
                    val wanted = word.drop(6).lowercase(Locale.ROOT)
                    { note: Note -> note.title.lowercase(Locale.ROOT).contains(wanted) }
                }
                lower.startsWith("body:") && word.length > 5 -> {
                    val wanted = word.drop(5).lowercase(Locale.ROOT)
                    { note: Note -> note.body.lowercase(Locale.ROOT).contains(wanted) }
                }
                lower.startsWith("before:") || lower.startsWith("after:") -> {
                    val before = lower.startsWith("before:")
                    val suffix = word.substringAfter(':')
                    val date = runCatching { LocalDate.parse(suffix) }.getOrNull()
                    if (date == null) literal(word)
                    else {
                        val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                        if (before) ({ note: Note -> note.updatedAt < millis })
                        else ({ note: Note -> note.updatedAt >= millis })
                    }
                }
                else -> literal(word)
            }
            rules.add(if (excluded) ({ note: Note -> !rule(note) }) else rule)
        }
        return ParsedNoteQuery(scope, rules)
    }

    private fun literal(value: String): (Note) -> Boolean {
        val term = value.lowercase(Locale.ROOT)
        return { note ->
            note.title.lowercase(Locale.ROOT).contains(term) ||
                note.body.lowercase(Locale.ROOT).contains(term) ||
                note.tags.any { it.lowercase(Locale.ROOT).contains(term) }
        }
    }

    /** Supports quoted phrases, quoted field values and escaped quotation marks. */
    internal fun tokenize(value: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var escaped = false
        fun flush() {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
        }
        for (char in value.take(MAX_QUERY_LENGTH)) {
            when {
                escaped -> { current.append(char); escaped = false }
                char == '\\' && quoted -> escaped = true
                char == '"' -> quoted = !quoted
                char.isWhitespace() && !quoted -> flush()
                else -> current.append(char)
            }
            if (tokens.size >= MAX_CLAUSES) break
        }
        if (escaped) current.append('\\')
        flush()
        return tokens.take(MAX_CLAUSES)
    }
}
