package dev.juuri.noma.transfer

import dev.juuri.noma.domain.Note
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Plaintext, versioned interchange format. A backup is intentionally NOT encrypted.
 * Parsing is bounded and never mutates storage.
 */
object NoteTransferCodec {
    const val BACKUP_VERSION = 1
    const val MAX_BYTES = 32 * 1024 * 1024
    const val MAX_NOTES = 5_000
    const val MAX_TITLE_LENGTH = 16_384
    const val MAX_BODY_LENGTH = 1_000_000

    fun encode(notes: List<Note>, exportedAt: Long = System.currentTimeMillis()): ByteArray {
        require(notes.size <= MAX_NOTES) { "Too many notes to export" }
        require(exportedAt >= 0) { "Invalid export timestamp" }
        val ids = HashSet<String>()
        val entries = JSONArray()
        for (note in notes) {
            validate(note)
            require(ids.add(note.id)) { "Duplicate note ID" }
            entries.put(JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("body", note.body)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            })
        }
        val data = JSONObject().apply {
            put("format", "noma-notes")
            put("schemaVersion", BACKUP_VERSION)
            put("exportedAt", exportedAt)
            put("notes", entries)
        }.toString(2).toByteArray(StandardCharsets.UTF_8)
        require(data.size <= MAX_BYTES) { "Backup exceeds size limit" }
        return data
    }

    fun decode(bytes: ByteArray): List<Note> {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Unsupported backup size" }
        val raw = bytes.toString(StandardCharsets.UTF_8).removePrefix("\uFEFF")
        val root = JSONObject(raw)
        require(root.getString("format") == "noma-notes") { "Not a Noma backup" }
        require(root.getInt("schemaVersion") == BACKUP_VERSION) {
            "Unsupported backup version"
        }
        val entries = root.getJSONArray("notes")
        require(entries.length() <= MAX_NOTES) { "Too many notes in backup" }
        val ids = HashSet<String>()
        return buildList {
            for (index in 0 until entries.length()) {
                val item = entries.getJSONObject(index)
                val note = Note(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    body = item.getString("body"),
                    isPinned = item.getBoolean("isPinned"),
                    isArchived = item.getBoolean("isArchived"),
                    createdAt = strictTimestamp(item, "createdAt"),
                    updatedAt = strictTimestamp(item, "updatedAt")
                )
                validate(note)
                require(ids.add(note.id)) { "Duplicate note ID in backup" }
                add(note)
            }
        }
    }

    private fun strictTimestamp(item: JSONObject, key: String): Long {
        val value = item.get(key)
        require(value is Number) { "Invalid timestamp" }
        return value.toLong()
    }

    private fun validate(note: Note) {
        require(note.id.isNotBlank() && note.id.length <= 128) { "Invalid note ID" }
        require(note.title.length <= MAX_TITLE_LENGTH) { "Note title too long" }
        require(note.body.length <= MAX_BODY_LENGTH) { "Note content too long" }
        require(note.createdAt >= 0 && note.updatedAt >= 0) { "Invalid note timestamp" }
    }

    /** One markdown file is a single note, with optional first-line H1. */
    fun markdownToNote(text: String, filename: String, now: Long): Note {
        require(text.length <= MAX_BODY_LENGTH) { "Text file too large" }
        val normalized = text.removePrefix("\uFEFF")
        val first = normalized.lineSequence().firstOrNull().orEmpty()
        val hasHeading = first.startsWith("# ") && first.drop(2).trim().isNotBlank()
        val title = if (hasHeading) first.drop(2).trim() else safeTitle(filename)
        val body = if (hasHeading) {
            normalized.substringAfter("\n", "").trimStart('\r', '\n')
        } else normalized
        return freshNote(title, body, now)
    }

    fun textToNote(text: String, filename: String, now: Long): Note =
        freshNote(safeTitle(filename), text.removePrefix("\uFEFF"), now)

    private fun freshNote(title: String, body: String, now: Long): Note {
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAt = now,
            updatedAt = now
        )
        validate(note)
        return note
    }

    fun toMarkdown(note: Note): String {
        validate(note)
        val heading = note.title.trim().replace("\n", " ").replace("\r", " ")
        return if (heading.isEmpty()) note.body else "# $heading\n\n${note.body}"
    }

    fun toText(note: Note): String {
        validate(note)
        return if (note.title.isBlank()) note.body
        else "${note.title}\n\n${note.body}"
    }

    fun safeTitle(filename: String): String {
        val base = filename.substringAfterLast('/').substringBeforeLast('.', filename)
        return base.trim().take(256).ifBlank { "Imported note" }
    }
}
