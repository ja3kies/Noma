package dev.juuri.noma.domain

/** Cursor-aware Markdown changes. UI state and database content stay plain text. */
data class MarkdownEdit(val text: String, val start: Int, val end: Int)

object MarkdownEdits {
    fun bold(text: String, start: Int, end: Int): MarkdownEdit {
        val lo = minOf(start, end).coerceIn(0, text.length)
        val hi = maxOf(start, end).coerceIn(lo, text.length)
        val selected = text.substring(lo, hi)
        val wrapper = "**$selected**"
        val next = text.replaceRange(lo, hi, wrapper)
        return MarkdownEdit(next, lo + 2, lo + 2 + selected.length)
    }

    fun heading(text: String, cursor: Int): MarkdownEdit =
        toggleLinePrefix(text, cursor, "## ")

    fun bullet(text: String, cursor: Int): MarkdownEdit =
        toggleLinePrefix(text, cursor, "- ")

    fun checklist(text: String, cursor: Int): MarkdownEdit =
        toggleLinePrefix(text, cursor, "- [ ] ")

    private fun toggleLinePrefix(text: String, cursor: Int, marker: String): MarkdownEdit {
        val pos = cursor.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let {
            if (it < 0) 0 else it + 1
        }
        val already = text.startsWith(marker, start)
        val updated = if (already) text.removeRange(start, start + marker.length)
            else text.substring(0, start) + marker + text.substring(start)
        val shift = if (already) -marker.length else marker.length
        val newPos = (pos + shift).coerceIn(start, updated.length)
        return MarkdownEdit(updated, newPos, newPos)
    }
}
