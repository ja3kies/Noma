package dev.juuri.noma.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesImportPreviewTest {
    private fun note(id: String) = Note(id, "Title", "Body", false, 1L, 2L)

    @Test fun countsOnlyRealIdCollisionsNotMatchingTitles() {
        val existing = listOf(note("a"), note("b"))
        val incoming = listOf(note("b"), note("c"), note("d"))
        assertEquals(1, NotesImportPreview.duplicateCount(existing, incoming))
    }

    @Test fun emptyListsHaveNoCollisions() {
        assertEquals(0, NotesImportPreview.duplicateCount(emptyList(), listOf(note("a"))))
        assertEquals(0, NotesImportPreview.duplicateCount(listOf(note("a")), emptyList()))
    }

    @Test fun thousandsOfNotesRemainCorrectWithoutNestedSearch() {
        val existing = (0 until 5_000).map { note("id-$it") }
        val incoming = (4_000 until 9_000).map { note("id-$it") }
        assertEquals(1_000, NotesImportPreview.duplicateCount(existing, incoming))
        assertEquals(5_000, existing.size)
        assertEquals(5_000, incoming.size)
    }

    @Test fun importingSameIdNeverChangesExistingEntryInPreview() {
        val original = note("shared").copy(title = "Original")
        val modified = original.copy(title = "Different")
        assertEquals(1, NotesImportPreview.duplicateCount(listOf(original), listOf(modified)))
        assertEquals("Original", original.title)
    }
}
