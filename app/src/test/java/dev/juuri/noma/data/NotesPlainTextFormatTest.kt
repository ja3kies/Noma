package dev.juuri.noma.data

import org.junit.Assert.*
import org.junit.Test

class NotesPlainTextFormatTest {
    @Test fun textImportPreservesUnicodeNewlinesAndContent() {
        val text = "Bonjour 🦊\n# heading\n- [ ] test\n"
        val note = NotesPlainTextFormat.decode(text.toByteArray(Charsets.UTF_8), "my-notes.md", 42L, "id")
        assertEquals("my-notes", note.title)
        assertEquals(text, note.body)
        assertEquals(42L, note.createdAt)
        assertEquals("id", note.id)
        assertFalse(note.isArchived)
    }
    @Test fun utf8BomIsRemoved() {
        val note = NotesPlainTextFormat.decode("\uFEFFHello".toByteArray(), "a.txt", 1L, "x")
        assertEquals("Hello", note.body)
    }
    @Test fun malformedUtf8IsRejected() {
        try {
            NotesPlainTextFormat.decode(byteArrayOf(0xC3.toByte(), 0x28), "broken.txt")
            fail("Malformed UTF-8 accepted")
        } catch (_: java.nio.charset.CharacterCodingException) { }
    }
    @Test fun oversizedFileIsRejectedWithoutAllocatingNote() {
        try {
            NotesPlainTextFormat.decode(ByteArray(NotesPortableFormat.MAX_IMPORT_BYTES + 1), "big.md")
            fail("Oversize accepted")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun pathSegmentsCannotBecomeNoteTitle() {
        val note = NotesPlainTextFormat.decode(byteArrayOf(), "../../private/idea.txt", 0, "x")
        assertEquals("idea", note.title)
    }
}
