package dev.juuri.noma.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Hard byte cap for untrusted SAF document providers. */
object NotesDocumentIo {
    fun readBounded(input: InputStream, maxBytes: Int = NotesPortableFormat.MAX_IMPORT_BYTES): ByteArray {
        require(maxBytes >= 0) { "Invalid document size limit" }
        val result = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var zeroReads = 0
        while (true) {
            val count = input.read(chunk, 0, chunk.size)
            if (count < 0) break
            if (count == 0) {
                if (++zeroReads >= 10) error("Document provider made no progress")
                continue
            }
            zeroReads = 0
            require(count <= maxBytes - result.size()) { "Document exceeds size limit" }
            result.write(chunk, 0, count)
        }
        return result.toByteArray()
    }
}
