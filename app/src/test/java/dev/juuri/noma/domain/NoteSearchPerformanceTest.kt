package dev.juuri.noma.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchPerformanceTest {
    private fun note(body: String, title: String = "") =
        Note("search-preview", title, body, false, 1L, 1L)

    @Test fun whitespaceTermsPreserveAccentInsensitiveMatching() {
        assertEquals(listOf("cafe", "oeuvre"), NoteSearch.terms("  CAFÉ\t œuvre\n"))
        assertTrue(NoteSearch.matches(note("un café et une œuvre"), "cafe oeuvre"))
    }

    @Test fun matchingPreviewKeepsFirstPerfectMatch() {
        val body = "not related\nFirst café maison\nSecond cafe maison"
        val preview = NoteSearch.matchingPreview(note(body), "cafe maison")
        assertEquals("First café maison", preview)
    }

    @Test fun matchingPreviewUsesBestAvailableLineWhenNoPerfectMatch() {
        val body = "café\nune maison\nmaison et café\nmaison"
        assertEquals("maison et café", NoteSearch.matchingPreview(note(body), "cafe maison absent"))
    }

    @Test fun matchingPreviewHandlesLongTextWithoutChangingNote() {
        val body = buildString {
            repeat(1500) { append("irrelevant line number ").append(it).append('\n') }
            append("résultat important et rapide\n")
            repeat(1500) { append("another unimportant line\n") }
        }
        val entry = note(body)
        val preview = NoteSearch.matchingPreview(entry, "resultat important")
        assertTrue(preview.contains("résultat important"))
        assertEquals(body, entry.body)
        assertFalse(preview.contains("another unimportant"))
    }

    @Test fun rankedSearchKeepsStableTieBreakerAndPinnedOrder() {
        val entries = listOf(
            Note("z", "Café notes", "Body", false, 1L, 8L),
            Note("a", "Café notes", "Body", false, 1L, 8L),
            Note("pin", "Café notes", "Body", true, 1L, 3L),
            Note("exact", "cafe", "Body", false, 1L, 1L)
        )
        val repo = object : NotesRepository {
            override val notes = kotlinx.coroutines.flow.MutableStateFlow<NotesSnapshot>(
                NotesSnapshot.Ready(entries)
            )
            override suspend fun refresh() = Unit
            override suspend fun save(note: Note) = Unit
            override suspend fun delete(id: String) = Unit
            override suspend fun importMissing(notes: List<Note>): Int = 0
        }
        val service = NotesService(repo)
        assertEquals(
            listOf("exact", "pin", "a", "z"),
            service.visible(entries, "cafe", false, sort = NoteSort.RECENT).map { it.id }
        )
    }

    @Test fun titleSortNormalizesAccentsAndPreservesRecentTieBreaker() {
        val entries = listOf(
            Note("z", "Éclair", "Body", false, 1L, 10L),
            Note("a", "eclair", "Body", false, 1L, 20L),
            Note("b", "Banane", "Body", false, 1L, 30L)
        )
        val repo = object : NotesRepository {
            override val notes = kotlinx.coroutines.flow.MutableStateFlow<NotesSnapshot>(
                NotesSnapshot.Ready(entries)
            )
            override suspend fun refresh() = Unit
            override suspend fun save(note: Note) = Unit
            override suspend fun delete(id: String) = Unit
            override suspend fun importMissing(notes: List<Note>): Int = 0
        }
        val service = NotesService(repo)
        assertEquals(
            listOf("b", "a", "z"),
            service.visible(entries, "", false, sort = NoteSort.TITLE).map { it.id }
        )
    }

    @Test fun cachedRelevanceMatchesDirectRelevanceAcrossWhitespaceAndLigatures() {
        val entry = note("Une œuvre moderne et du café", title = "Café et œuvres")
        val queries = listOf(
            "CAFE", "  café   œuvres  ", "œuvres\tcafe",
            "introuvable", "   ", "cafe et", "OEUVRES"
        )
        for (query in queries) {
            val words = NoteSearch.terms(query)
            assertEquals(
                "Unexpected cached score for: $query",
                NoteSearch.relevance(entry, query),
                NoteSearch.relevanceForTerms(entry, words, words.joinToString(" "))
            )
        }
    }

    @Test fun largeNotebookSearchDoesNotMutateInputAndKeepsStableResults() {
        val entries = (0 until 2_000).map { index ->
            Note(
                "id-$index",
                if (index % 10 == 0) "Café entry $index" else "Regular entry $index",
                if (index % 10 == 0) "Une œuvre intéressante" else "Ordinary content",
                index == 100,
                1L,
                index.toLong()
            )
        }
        val original = entries.toList()
        val repo = object : NotesRepository {
            override val notes = kotlinx.coroutines.flow.MutableStateFlow<NotesSnapshot>(
                NotesSnapshot.Ready(entries)
            )
            override suspend fun refresh() = Unit
            override suspend fun save(note: Note) = Unit
            override suspend fun delete(id: String) = Unit
            override suspend fun importMissing(notes: List<Note>): Int = 0
        }
        val results = NotesService(repo).visible(
            entries, "CAFÉ ŒUVRE", pinnedOnly = false, sort = NoteSort.TITLE
        )
        assertEquals(200, results.size)
        assertEquals("id-100", results.first().id)
        assertEquals(original, entries)
    }

    @Test fun normalizationPreservesEmojiPositionsAndAccentedHighlightRanges() {
        val line = "🦊 café et œuvre"
        val ranges = NoteSearch.matchRanges(line, "CAFE ŒUVRE")
        assertEquals(2, ranges.size)
        assertEquals("café", line.substring(ranges[0].first, ranges[0].last + 1))
        assertEquals("œuvre", line.substring(ranges[1].first, ranges[1].last + 1))
    }

    @Test fun matchedPreviewReturnsFallbackWhenNoBodyLineMatches() {
        val entry = note("A short body", title = "Café")
        assertEquals("A short body", NoteSearch.matchingPreview(entry, "cafe"))
    }
}
