package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

/** Independent regression actions for the Unicode search hot path. */
class UnicodeSearchHardeningTest {
    private fun note(title: String = "", body: String = "") =
        Note("unicode", title, body, false, 1L, 1L)

    @Test fun frenchAcuteAccentFolds() {
        assertEquals("cafe", NoteSearch.normalize("CAFÉ"))
    }
    @Test fun frenchGraveAccentFolds() {
        assertEquals("eleve", NoteSearch.normalize("Élève"))
    }
    @Test fun frenchCedillaFolds() {
        assertEquals("garcon", NoteSearch.normalize("garçon"))
    }
    @Test fun oeligatureExpands() {
        assertEquals("oeuvre", NoteSearch.normalize("ŒUVRE"))
    }
    @Test fun aeligatureExpands() {
        assertEquals("aether", NoteSearch.normalize("Æther"))
    }
    @Test fun germanSharpSExpands() {
        assertEquals("strasse", NoteSearch.normalize("Straße"))
    }
    @Test fun nordicOExpands() {
        assertEquals("oslo", NoteSearch.normalize("Øslo"))
    }
    @Test fun polishLExpands() {
        assertEquals("lodz", NoteSearch.normalize("Łódź"))
    }
    @Test fun unicodeTermsNormalizeWhitespace() {
        assertEquals(listOf("cafe", "oeuvre"), NoteSearch.terms("  CAFÉ\n\tŒuvre  "))
    }
    @Test fun crossFieldSearchMatchesWords() {
        assertTrue(NoteSearch.matches(note("Café", "une œuvre"), "oeuvre cafe"))
    }
    @Test fun allTermsAreRequired() {
        assertFalse(NoteSearch.matches(note("Café", "une œuvre"), "oeuvre impossible"))
    }
    @Test fun exactTitleRankBeatsPrefixRank() {
        assertTrue(NoteSearch.relevance(note("café"), "cafe") <
            NoteSearch.relevance(note("café réunion"), "cafe"))
    }
    @Test fun titlePrefixRankBeatsBodyOnlyRank() {
        assertTrue(NoteSearch.relevance(note("Café maison"), "cafe") <
            NoteSearch.relevance(note("Autre", "café"), "cafe"))
    }
    @Test fun emojiDoesNotShiftHighlightCoordinates() {
        val source = "🦊 café"
        val ranges = NoteSearch.matchRanges(source, "CAFE")
        assertEquals(1, ranges.size)
        assertEquals("café", source.substring(ranges[0].first, ranges[0].last + 1))
    }
    @Test fun matchedPreviewKeepsFirstBestLine() {
        assertEquals("Premier café", NoteSearch.matchingPreview(
            note(body = "Premier café\nSecond café"), "cafe"))
    }
    @Test fun previewDoesNotModifyOriginalNote() {
        val original = note(body = "a".repeat(300) + " café")
        NoteSearch.matchingPreview(original, "cafe")
        assertEquals("a".repeat(300) + " café", original.body)
    }
    @Test fun matchLimitRemainsEnforced() {
        assertTrue(NoteSearch.matchRanges("café ".repeat(100), "cafe", maxMatches = 3).size <= 3)
    }
    @Test fun emptyQueryDoesNotHighlightAnything() {
        assertTrue(NoteSearch.matchRanges("café", " ").isEmpty())
    }
}
