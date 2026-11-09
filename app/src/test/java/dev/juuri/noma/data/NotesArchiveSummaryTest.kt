package dev.juuri.noma.data

import dev.juuri.noma.domain.Note
import org.junit.Assert.*
import org.junit.Test

class NotesArchiveSummaryTest {
    @Test fun emptyBackupSummaryIsValidAndUnambiguous() {
        assertEquals(
            NotesArchiveSummary(0, 0, 0, null, null, 0),
            NotesArchiveSummary.fromNotes(emptyList())
        )
    }

    @Test fun summaryCountsPinnedAndArchivedWithoutWritingAnything() {
        val original = listOf(
            Note("a", "A", "café", true, 10L, 20L, false),
            Note("b", "B", "hello world", false, 5L, 60L, true),
            Note("c", "C", "", true, 6L, 40L, true)
        )
        val before = original.toList()
        assertEquals(
            NotesArchiveSummary(3, 2, 2, 60L, 5L, 15L),
            NotesArchiveSummary.fromNotes(original)
        )
        assertEquals(before, original)
    }

    @Test fun countsCharactersWithLongWithoutNarrowing() {
        val notes = listOf(
            Note("id", "", "🦊", false, 1L, 1L, false)
        )
        assertEquals(2L, NotesArchiveSummary.fromNotes(notes).totalBodyCharacters)
    }
}
