package dev.juuri.noma.data

import dev.juuri.noma.domain.Note
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Portable Noma archive v1. This is a lossless interchange format, not an
 * encrypted backup. Do not share exported files containing private notes.
 */
object NotesPortableFormat {
    const val VERSION = 1
    const val MAX_IMPORT_BYTES = 16 * 1024 * 1024
    const val MAX_NOTES = 10_000

    fun encode(notes: List<Note>): ByteArray {
        require(notes.size <= MAX_NOTES) { "Too many notes" }
        require(notes.map { it.id }.toSet().size == notes.size) { "Duplicate note IDs" }
        val entries = JSONArray()
        notes.sortedBy { it.id }.forEach { note ->
            entries.put(JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("body", note.body)
                put("pinned", note.isPinned)
                put("archived", note.isArchived)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            })
        }
        return JSONObject().apply {
            put("format", "noma-notes")
            put("version", VERSION)
            put("notes", entries)
        }.toString(2).toByteArray(StandardCharsets.UTF_8).also {
            require(it.size <= MAX_IMPORT_BYTES) { "Archive exceeds size limit" }
        }
    }

    fun decode(input: ByteArray): List<Note> {
        require(input.size <= MAX_IMPORT_BYTES) { "Archive exceeds size limit" }
        val decoder = StandardCharsets.UTF_8.newDecoder()
        val decoded = decoder.decode(java.nio.ByteBuffer.wrap(input)).toString()
        val root = JSONObject(decoded)
        require(root.getString("format") == "noma-notes") { "Unsupported archive format" }
        require(root.getInt("version") == VERSION) { "Unsupported archive version" }
        val array = root.getJSONArray("notes")
        require(array.length() <= MAX_NOTES) { "Too many notes" }
        val ids = HashSet<String>()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val id = obj.getString("id")
            require(id.isNotBlank() && id.length <= 256 && ids.add(id)) { "Invalid or duplicate note ID" }
            val created = obj.getLong("createdAt")
            val updated = obj.getLong("updatedAt")
            require(created >= 0 && updated >= created) { "Invalid timestamps" }
            val title = obj.getString("title")
            val body = obj.getString("body")
            require(title.length <= 100_000 && body.length <= 1_000_000) { "Note too large" }
            Note(
                id = id,
                title = title,
                body = body,
                isPinned = obj.getBoolean("pinned"),
                createdAt = created,
                updatedAt = updated,
                isArchived = obj.getBoolean("archived")
            )
        }
    }

    fun toText(note: Note): String =
        listOf(note.title.trim(), note.body).filter(String::isNotEmpty).joinToString("\n\n")

    fun toMarkdown(note: Note): String {
        val title = note.title.trim()
        return if (title.isEmpty()) note.body
        else "# " + title.replace("\n", " ") + "\n\n" + note.body
    }

    /** A conservative basename, never a path. The user chooses the final SAF destination. */
    fun suggestedName(note: Note, extension: String): String {
        require(extension in setOf("txt", "md", "json"))
        val stem = note.displayTitle.ifBlank { "note" }
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim(' ', '.')
            .take(64)
            .ifBlank { "note" }
        return "$stem.$extension"
    }
}
