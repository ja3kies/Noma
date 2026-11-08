package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

class MarkdownEditsTest {
    @Test fun wrapsSelectedTextInBoldMarkers() {
        val result = MarkdownEdits.bold("hello world", 6, 11)
        assertEquals("hello **world**", result.text)
        assertEquals(8, result.start)
        assertEquals(13, result.end)
    }

    @Test fun insertsBalancedBoldMarkersWithoutSelection() {
        val result = MarkdownEdits.bold("hello", 5, 5)
        assertEquals("hello****", result.text)
        assertEquals(7, result.start)
        assertEquals(7, result.end)
    }

    @Test fun turnsCurrentLineIntoHeading() {
        val result = MarkdownEdits.heading("hello\nworld", 8)
        assertEquals("hello\n## world", result.text)
    }

    @Test fun headingCanBeToggledOff() {
        assertEquals("hello", MarkdownEdits.heading("## hello", 5).text)
    }

    @Test fun bulletCanBeToggledOff() {
        assertEquals("hello", MarkdownEdits.bullet("- hello", 4).text)
    }

    @Test fun addingChecklistTargetsCurrentLine() {
        assertEquals("one\n- [ ] two", MarkdownEdits.checklist("one\ntwo", 6).text)
    }

    @Test fun formatCannotCrashOnOutOfRangeCursor() {
        assertEquals("## hello", MarkdownEdits.heading("hello", 999).text)
    }

    @Test fun formattingPreservesMultilingualText() {
        assertEquals("**日本語**", MarkdownEdits.bold("日本語", 0, 3).text)
    }

    @Test fun formattingPreservesUnrelatedLines() {
        val original = "first\nsecond\nthird"
        assertEquals("first\n- second\nthird", MarkdownEdits.bullet(original, 10).text)
    }
}
