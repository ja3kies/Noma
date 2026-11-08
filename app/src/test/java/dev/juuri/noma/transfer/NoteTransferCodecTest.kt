package dev.juuri.noma.transfer

import dev.juuri.noma.domain.Note
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NoteTransferCodecTest {
    private val note = Note(
        id = "note-1",
        title = "French & 日本語 \"quotes\"",
        body = "Line 1\nLine 2\t🚀",
        isPinned = true,
        isArchived = true,
        createdAt = 123L,
        updatedAt = 456L
    )

    @Test fun jsonRoundTripPreservesEveryField() {
        val restored = NoteTransferCodec.decode(NoteTransferCodec.encode(listOf(note), 99L))
        assertEquals(listOf(note), restored)
    }

    @Test fun emptyCollectionCanBeBackedUp() {
        assertTrue(NoteTransferCodec.decode(NoteTransferCodec.encode(emptyList())).isEmpty())
    }

    @Test fun escapingAndUnicodeAreValidUtf8() {
        val bytes = NoteTransferCodec.encode(listOf(note))
        assertTrue(bytes.toString(Charsets.UTF_8).contains("日本語"))
        assertEquals(note.body, NoteTransferCodec.decode(bytes).single().body)
    }

    @Test fun rejectsWrongFileFormat() {
        val data = NoteTransferCodec.encode(listOf(note))
        val changed = JSONObject(data.toString(Charsets.UTF_8)).put("format", "other")
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(changed.toString().toByteArray())
        }
    }

    @Test fun rejectsFutureBackupVersion() {
        val root = JSONObject(NoteTransferCodec.encode(listOf(note)).toString(Charsets.UTF_8))
        root.put("schemaVersion", 999)
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(root.toString().toByteArray())
        }
    }

    @Test fun rejectsDuplicateNoteIdsInBackup() {
        val root = JSONObject(NoteTransferCodec.encode(listOf(note, note.copy(id = "note-2"))).toString(Charsets.UTF_8))
        root.getJSONArray("notes").getJSONObject(1).put("id", "note-1")
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(root.toString().toByteArray())
        }
    }

    @Test fun rejectsDuplicateIdsOnExport() {
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.encode(listOf(note, note))
        }
    }

    @Test fun rejectsInvalidTimestamps() {
        val root = JSONObject(NoteTransferCodec.encode(listOf(note)).toString(Charsets.UTF_8))
        root.getJSONArray("notes").getJSONObject(0).put("createdAt", -10)
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(root.toString().toByteArray())
        }
    }

    @Test fun rejectsUnsupportedSize() {
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(ByteArray(NoteTransferCodec.MAX_BYTES + 1))
        }
    }

    @Test fun rejectsOversizedNoteContent() {
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.encode(listOf(note.copy(body = "x".repeat(NoteTransferCodec.MAX_BODY_LENGTH + 1))))
        }
    }

    @Test fun markdownRoundTripKeepsBodyAndHeading() {
        val content = NoteTransferCodec.toMarkdown(note)
        val imported = NoteTransferCodec.markdownToNote(content, "unused.md", 1000L)
        assertEquals(note.title, imported.title)
        assertEquals(note.body, imported.body)
        assertEquals(1000L, imported.createdAt)
        assertFalse(imported.isArchived)
    }

    @Test fun markdownWithoutHeadingUsesFileName() {
        val imported = NoteTransferCodec.markdownToNote("hello", "idea.md", 10L)
        assertEquals("idea", imported.title)
        assertEquals("hello", imported.body)
    }

    @Test fun plainTextImportUsesFilenameAndFullBody() {
        val imported = NoteTransferCodec.textToNote("first\nsecond", "todo.txt", 10L)
        assertEquals("todo", imported.title)
        assertEquals("first\nsecond", imported.body)
    }

    @Test fun malformedUtf8CannotCorruptBackupSilently() {
        assertThrows(java.nio.charset.MalformedInputException::class.java) {
            NoteTransferCodec.decode(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }

    @Test fun fractionalTimestampsAreRejected() {
        val root = JSONObject(NoteTransferCodec.encode(listOf(note)).toString(Charsets.UTF_8))
        root.getJSONArray("notes").getJSONObject(0).put("updatedAt", 1.5)
        assertThrows(IllegalArgumentException::class.java) {
            NoteTransferCodec.decode(root.toString().toByteArray())
        }
    }

    @Test fun markdownRetainsLeadingBlankLines() {
        val original = note.copy(body = "\n\nFirst line")
        val decoded = NoteTransferCodec.markdownToNote(
            NoteTransferCodec.toMarkdown(original), "anything.md", 50L
        )
        assertEquals(original.body, decoded.body)
    }

    @Test fun utf8BomIsAccepted() {
        val backup = "\uFEFF" + NoteTransferCodec.encode(listOf(note)).toString(Charsets.UTF_8)
        assertEquals(note, NoteTransferCodec.decode(backup.toByteArray()).single())
    }

    @Test fun titleSanitizerNeverUsesPath() {
        assertEquals("notes", NoteTransferCodec.safeTitle("/docs/private/notes.md"))
        assertEquals("Imported note", NoteTransferCodec.safeTitle(" "))
    }
}
