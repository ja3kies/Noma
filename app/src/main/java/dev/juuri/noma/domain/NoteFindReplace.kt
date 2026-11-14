package dev.juuri.noma.domain

/**
 * Replacement operations for local note text. Both methods are pure: callers
 * must submit the resulting body through the same autosave revision queue as
 * ordinary edits. Matching is case-insensitive, non-overlapping UTF-16 text;
 * no regular expressions or special replacement syntax are evaluated.
 */
object NoteFindReplace {
    /**
     * Find only non-overlapping occurrences, just like count() and replaceAll().
     * Starting a search in the middle of a match must not return a phantom
     * overlapping occurrence (for example "aa" at offset 1 in "aaaa").
     * Wrap to the first/last match when there is no match beyond the caret.
     */
    fun nextMatch(text: String, needle: String, from: Int, backwards: Boolean = false): IntRange? {
        if (needle.isBlank() || needle.length > text.length) return null
        var first = -1
        var last = -1
        var selected = -1
        var offset = 0
        while (offset <= text.length - needle.length) {
            val found = text.indexOf(needle, startIndex = offset, ignoreCase = true)
            if (found < 0) break
            if (first < 0) first = found
            last = found
            if (backwards) {
                if (found <= from) selected = found
            } else if (selected < 0 && found >= from) {
                selected = found
            }
            offset = found + needle.length
        }
        val chosen = if (selected >= 0) selected else if (backwards) last else first
        return if (chosen < 0) null else chosen until chosen + needle.length
    }

    fun count(text: String, needle: String): Int {
        if (needle.isBlank()) return 0
        var matches = 0
        var index = 0
        while (index < text.length) {
            val found = text.indexOf(needle, startIndex = index, ignoreCase = true)
            if (found < 0) break
            matches++
            index = found + needle.length
        }
        return matches
    }

    fun replaceFirst(text: String, needle: String, replacement: String): String {
        if (needle.isBlank()) return text
        val index = text.indexOf(needle, ignoreCase = true)
        if (index < 0) return text
        return buildString(text.length - needle.length + replacement.length) {
            append(text, 0, index)
            append(replacement)
            append(text, index + needle.length, text.length)
        }
    }

    fun replaceAll(text: String, needle: String, replacement: String): String {
        if (needle.isBlank()) return text
        val first = text.indexOf(needle, ignoreCase = true)
        if (first < 0) return text
        return buildString(text.length) {
            var from = 0
            var match = first
            while (match >= 0) {
                append(text, from, match)
                append(replacement)
                from = match + needle.length
                match = text.indexOf(needle, startIndex = from, ignoreCase = true)
            }
            append(text, from, text.length)
        }
    }
}
