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

    @Test fun matchedPreviewReturnsFallbackWhenNoBodyLineMatches() {
        val entry = note("A short body", title = "Café")
        assertEquals("A short body", NoteSearch.matchingPreview(entry, "cafe"))
    }
}
