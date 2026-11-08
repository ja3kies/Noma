package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

class NoteTagsTest {
    private val note = Note("id", "Title", "Body", false, 1L, 1L)

    @Test fun normalize() {
        assertEquals("home projects", NoteTags.normalize(" #home  projects  "))
    }

    @Test fun blanksAreIgnored() {
        assertEquals(note, NoteTags.add(note, "   #   "))
    }

    @Test fun duplicatesAreCaseInsensitive() {
        val once = NoteTags.add(note, "Books")
        assertEquals(once, NoteTags.add(once, "BOOKS"))
    }

    @Test fun tagsDoNotChangeOtherMetadata() {
        val old = note.copy(isPinned = true, isArchived = true)
        val next = NoteTags.add(old, "Home")
        assertEquals(old.createdAt, next.createdAt)
        assertTrue(next.isPinned)
        assertTrue(next.isArchived)
    }

    @Test fun removingIsCaseInsensitive() {
        assertEquals(listOf("Home"), NoteTags.remove(
            note.copy(tags = listOf("Work", "Home")), "WORK"
        ).tags)
    }

    @Test fun lengthsAreBounded() {
        assertEquals(NoteTags.MAX_LENGTH, NoteTags.normalize("x".repeat(200)).length)
    }

    @Test fun countLimitRejectsOverflow() {
        val full = note.copy(tags = (0 until NoteTags.MAX_COUNT).map { "tag$it" })
        assertThrows(IllegalArgumentException::class.java) { NoteTags.add(full, "extra") }
    }

    @Test fun enumerationExcludesWrongArchiveAndDeDuplicates() {
        val entries = listOf(
            note.copy(tags = listOf("Home", "Work")),
            note.copy(id = "second", tags = listOf("work", "Ideas")),
            note.copy(id = "archived", isArchived = true, tags = listOf("Private"))
        )
        assertEquals(listOf("Home", "Ideas", "Work"), NoteTags.all(entries, false))
        assertEquals(listOf("Private"), NoteTags.all(entries, true))
    }
}
