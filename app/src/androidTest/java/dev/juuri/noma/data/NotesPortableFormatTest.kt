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

    @Test fun invalidUtf8IsRejectedWithoutReplacementCharacters() {
        try {
            NotesPortableFormat.decode(byteArrayOf(0xC3.toByte(), 0x28))
            throw AssertionError("Malformed UTF-8 accepted")
        } catch (_: java.nio.charset.CharacterCodingException) { }
    }

    @Test fun rejectsTruncatedArchiveWithoutSideEffects() {
        val valid = NotesPortableFormat.encode(notes)
        try {
            NotesPortableFormat.decode(valid.copyOf(valid.size / 2))
            throw AssertionError("Truncated JSON accepted")
        } catch (_: org.json.JSONException) { }
    }

    @Test fun refusesInvalidExportTimestamps() {
        try {
            NotesPortableFormat.encode(listOf(notes[0].copy(updatedAt = 1L)))
            throw AssertionError("Timestamp mismatch accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun rejectsDuplicateNoteIdsInImportedArchive() {
        val bytes = NotesPortableFormat.encode(notes)
        val root = org.json.JSONObject(String(bytes, Charsets.UTF_8))
        val entries = root.getJSONArray("notes")
        entries.put(org.json.JSONObject(entries.getJSONObject(0).toString()))
        try {
            NotesPortableFormat.decode(root.toString().toByteArray(Charsets.UTF_8))
            throw AssertionError("Duplicate IDs accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun rejectsInvalidVersionEvenWhenNotesAreEmpty() {
        val root = org.json.JSONObject()
            .put("format", "noma-notes").put("version", 2)
            .put("notes", org.json.JSONArray())
        try {
            NotesPortableFormat.decode(root.toString().toByteArray())
            throw AssertionError("Future archive accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun archiveInspectorReportsStructureWithoutImportSideEffects() {
        val bytes = NotesPortableFormat.encode(notes)
        val summary = NotesArchiveSummary.inspect(bytes)
        assertEquals(2, summary.total)
        assertEquals(1, summary.pinned)
        assertEquals(1, summary.archived)
        assertEquals(20L, summary.mostRecentEdit)
        assertEquals(0L, summary.oldestCreation)
        assertEquals(notes.sumOf { it.body.length.toLong() }, summary.totalBodyCharacters)
    }

    @Test fun archiveInspectorRefusesUnknownVersion() {
        val root = org.json.JSONObject(String(NotesPortableFormat.encode(notes), Charsets.UTF_8))
        root.put("version", 202)
        try {
            NotesArchiveSummary.inspect(root.toString().toByteArray())
            throw AssertionError("Invalid version was marked readable")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun archiveInspectorRejectsCorruptedJson() {
        try {
            NotesArchiveSummary.inspect("{ invalid json".toByteArray())
            throw AssertionError("Malformed JSON was accepted")
        } catch (_: org.json.JSONException) { }
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
