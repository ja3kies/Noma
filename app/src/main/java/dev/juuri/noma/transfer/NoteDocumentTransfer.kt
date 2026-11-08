package dev.juuri.noma.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.juuri.noma.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Uses Storage Access Framework documents picked by the user.
 * No storage permission or direct filesystem paths are required.
 */
object NoteDocumentTransfer {
    suspend fun readNotes(context: Context, uri: Uri): List<Note> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }.orEmpty()
        val type = resolver.getType(uri).orEmpty()
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                require(out.size() <= NoteTransferCodec.MAX_BYTES) {
                    "Selected document is too large"
                }
            }
            out.toByteArray()
        } ?: error("Cannot open selected document")
        require(bytes.isNotEmpty()) { "Selected document is empty" }
        val plain = name.lowercase(java.util.Locale.ROOT)
        when {
            plain.endsWith(".json") || type == "application/json" ->
                NoteTransferCodec.decode(bytes)
            plain.endsWith(".md") || plain.endsWith(".markdown") || type == "text/markdown" ->
                listOf(NoteTransferCodec.markdownToNote(decodeUtf8(bytes), name, System.currentTimeMillis()))
            plain.endsWith(".txt") || type == "text/plain" ->
                listOf(NoteTransferCodec.textToNote(decodeUtf8(bytes), name, System.currentTimeMillis()))
            else -> error("Choose a Noma JSON backup, Markdown or TXT file")
        }
    }

    suspend fun writeBackup(context: Context, uri: Uri, notes: List<Note>) =
        writeBytes(context, uri, NoteTransferCodec.encode(notes))

    suspend fun writeNote(context: Context, uri: Uri, note: Note, markdown: Boolean) {
        val text = if (markdown) NoteTransferCodec.toMarkdown(note)
            else NoteTransferCodec.toText(note)
        writeBytes(context, uri, text.toByteArray(StandardCharsets.UTF_8))
    }

    private suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(bytes)
                output.flush()
            } ?: error("Cannot write selected document")
        }

    private fun decodeUtf8(bytes: ByteArray): String =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
}
