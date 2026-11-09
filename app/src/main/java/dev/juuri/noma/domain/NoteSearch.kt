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
            // NFD alone does not expand French ligatures or some European letters.
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("ß", "ss")
            .replace("ø", "o")
            .replace("ł", "l")

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

    /**
     * Smaller rank means a result is more obviously relevant to the typed
     * query. This only affects nonempty searches; the selected notebook sort
     * still provides a stable tie-breaker.
     */
    fun relevance(note: Note, query: String): Int {
        val phrase = normalize(query).trim().replace(Regex("\\s+"), " ")
        if (phrase.isEmpty()) return 0
        val title = normalize(note.title)
        val words = terms(query)
        return when {
            title == phrase -> 0
            title.startsWith(phrase) -> 1
            words.all(title::contains) -> 2
            words.any(title::contains) -> 3
            else -> 4
        }
    }

    /**
     * Original UTF-16 ranges for each visible match. Unlike lowercase-only
     * substring search this handles accents, ligatures and supplementary code
     * points without changing the source text or the editor selection.
     */
    fun matchRanges(text: String, query: String, maxMatches: Int = 24): List<IntRange> {
        val words = terms(query).distinct()
        if (words.isEmpty() || text.isEmpty() || maxMatches <= 0) return emptyList()
        val folded = StringBuilder()
        val starts = ArrayList<Int>()
        val ends = ArrayList<Int>()
        var index = 0
        while (index < text.length) {
            val point = Character.codePointAt(text, index)
            val width = Character.charCount(point)
            val representation = normalize(String(Character.toChars(point)))
            repeat(representation.length) {
                starts.add(index)
                ends.add(index + width)
            }
            folded.append(representation)
            index += width
        }
        val locations = ArrayList<IntRange>()
        for (word in words) {
            var from = 0
            while (locations.size < maxMatches) {
                val found = folded.indexOf(word, from)
                if (found < 0) break
                val last = found + word.length - 1
                if (found < starts.size && last < ends.size) {
                    locations.add(starts[found] until ends[last])
                }
                from = found + word.length
            }
            if (locations.size >= maxMatches) break
        }
        val ordered = locations.sortedWith(compareBy<IntRange> { it.first }.thenBy { it.last })
        if (ordered.isEmpty()) return ordered
        val result = ArrayList<IntRange>()
        for (range in ordered) {
            val previous = result.lastOrNull()
            if (previous != null && range.first <= previous.last + 1) {
                result[result.lastIndex] = previous.first..maxOf(previous.last, range.last)
            } else result.add(range)
        }
        return result
    }

    private const val PREVIEW_LIMIT = 160
    private const val PREVIEW_LEADING_CONTEXT = 48

    /**
     * A long body line should reveal the actual matching passage rather than
     * truncating it off screen. Return a bounded excerpt and preserve the original
     * text, including accents and capitalization.
     */
    private fun excerpt(line: String, words: List<String>): String {
        if (line.length <= PREVIEW_LIMIT) return line
        val folded = StringBuilder()
        val positions = ArrayList<Int>()
        var original = 0
        while (original < line.length) {
            val cp = Character.codePointAt(line, original)
            val piece = normalize(String(Character.toChars(cp)))
            repeat(piece.length) { positions.add(original) }
            folded.append(piece)
            original += Character.charCount(cp)
        }
        val found = words.map { folded.indexOf(it) }.filter { it >= 0 }.minOrNull()
        val hit = found?.let { positions.getOrNull(it) } ?: 0
        var start = if (hit <= PREVIEW_LEADING_CONTEXT) 0
            else (hit - PREVIEW_LEADING_CONTEXT).coerceAtMost(line.length - PREVIEW_LIMIT)
        // UTF-16 substring boundaries must not split a supplementary character.
        if (start > 0 && Character.isLowSurrogate(line[start])) start++
        var end = (start + PREVIEW_LIMIT).coerceAtMost(line.length)
        if (end < line.length && Character.isHighSurrogate(line[end - 1])) end--
        return (if (start > 0) "…" else "") +
            line.substring(start, end).trim() +
            (if (end < line.length) "…" else "")
    }

    /** The nearest body line with a match makes search results recognisable. */
    fun matchingPreview(note: Note, query: String): String {
        val words = terms(query)
        if (words.isEmpty()) return excerpt(note.preview, emptyList())
        val inTitle = normalize(note.title)
        val targetWords = words.filterNot(inTitle::contains).ifEmpty { words }
        val candidate = note.body.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { line ->
                val folded = normalize(line)
                line to targetWords.count(folded::contains)
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first ?: note.preview
        return excerpt(candidate, targetWords)
    }
}
