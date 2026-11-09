package dev.juuri.noma.data

import dev.juuri.noma.domain.Note
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Plain text and Markdown imports preserve the raw body without guessing its structure. */
object NotesPlainTextFormat {
    fun decode(bytes: ByteArray, filename: String?, now: Long = System.currentTimeMillis(),
               id: String = UUID.randomUUID().toString()): Note {
        require(bytes.size <= NotesPortableFormat.MAX_IMPORT_BYTES) { "File exceeds size limit" }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val raw = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        val body = raw.removePrefix("\uFEFF")
        require(body.length <= 1_000_000) { "Note too large" }
        require(now >= 0 && id.isNotBlank() && id.length <= 256) { "Invalid note identity" }
        val title = filename.orEmpty()
            .substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("(?i)\\.(md|markdown|txt)$"), "")
            .replace(Regex("[\\p{Cntrl}]"), " ")
            .trim().take(200)
        return Note(id = id, title = title, body = body, createdAt = now, updatedAt = now)
    }
}
