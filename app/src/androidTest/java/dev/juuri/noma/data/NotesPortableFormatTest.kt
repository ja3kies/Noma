package dev.juuri.noma.data

import dev.juuri.noma.domain.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android runtime JSON tests: use the platform org.json implementation.
 * These run on an emulator/device, not the JVM unit test task.
 */
class NotesPortableFormatTest {
    private val notes = listOf(
        Note("one", "Ideas ✨", "Line 1\nLine 2\n\"quotes\" & emoji 🦊", true, 10L, 20L, true),
        Note("two", "", "Body only", false, 0L, 1L, false)
    )

    @Test fun jsonRoundTripPreservesAllFields() {
        assertEquals(notes, NotesPortableFormat.decode(NotesPortableFormat.encode(notes)))
    }

    @Test fun jsonExportIsDeterministicAcrossInputOrdering() {
        val forward = NotesPortableFormat.encode(notes)
        val reversed = NotesPortableFormat.encode(notes.reversed())
        assertTrue(forward.contentEquals(reversed))
    }

    @Test fun rejectsDuplicateIdsWithoutWritingAnything() {
        try {
            NotesPortableFormat.encode(notes + notes[0])
            throw AssertionError("Duplicate IDs accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Duplicate"))
        }
    }

    @Test fun rejectsUnsupportedArchiveVersion() {
        val json = String(NotesPortableFormat.encode(notes)).replace(
            "\"version\": 1", "\"version\": 99"
        )
        try {
            NotesPortableFormat.decode(json.toByteArray())
            throw AssertionError("Future archive version accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("version"))
        }
    }

    @Test fun rejectsOversizedPayload() {
        try {
            NotesPortableFormat.decode(ByteArray(NotesPortableFormat.MAX_IMPORT_BYTES + 1))
            throw AssertionError("Oversized archive accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("size"))
        }
    }

    @Test fun textAndMarkdownPreserveBodyAndReadableTitle() {
        assertEquals("Ideas ✨\n\nLine 1\nLine 2\n\"quotes\" & emoji 🦊",
            NotesPortableFormat.toText(notes[0]))
        assertEquals("# Ideas ✨\n\nLine 1\nLine 2\n\"quotes\" & emoji 🦊",
            NotesPortableFormat.toMarkdown(notes[0]))
        assertEquals("Body only", NotesPortableFormat.toMarkdown(notes[1]))
    }

    @Test fun fileNameCannotContainPathSeparators() {
        val unsafe = notes[0].copy(title = "../private/notes:*?")
        val name = NotesPortableFormat.suggestedName(unsafe, "md")
        assertTrue(name.endsWith(".md"))
        assertFalse(name.contains("/"))
        assertFalse(name.contains("\\"))
        assertFalse(name.contains(":"))
    }
}
