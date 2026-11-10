package dev.juuri.noma.domain

/**
 * Replacement operations for local note text. Both methods are pure: callers
 * must submit the resulting body through the same autosave revision queue as
 * ordinary edits. Matching is case-insensitive, non-overlapping UTF-16 text;
 * no regular expressions or special replacement syntax are evaluated.
 */
object NoteFindReplace {
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
