package dev.juuri.noma.data

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Verify that a SAF document contains exactly the archive we attempted to save.
 * Content providers may permit writing but forbid reading the resulting URI.
 */
object NotesBackupVerification {
    enum class Status { VERIFIED, UNVERIFIED_PROVIDER }

    fun writeAndVerify(
        bytes: ByteArray,
        openWrite: () -> OutputStream?,
        openRead: () -> InputStream?
    ): Status {
        require(bytes.isNotEmpty() && bytes.size <= NotesPortableFormat.MAX_IMPORT_BYTES) {
            "Invalid backup archive size"
        }
        val output = openWrite() ?: error("Unable to open backup destination")
        output.use {
            it.write(bytes)
            it.flush()
        }
        val input = try { openRead() } catch (_: java.io.IOException) { null }
            ?: return Status.UNVERIFIED_PROVIDER
        input.use { stream ->
            val expected = MessageDigest.getInstance("SHA-256").digest(bytes)
            val actual = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var length = 0
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                length += n
                require(length <= NotesPortableFormat.MAX_IMPORT_BYTES) {
                    "Backup verification exceeded size limit"
                }
                actual.update(buffer, 0, n)
            }
            check(length == bytes.size && MessageDigest.isEqual(expected, actual.digest())) {
                "Backup contents differ from the exported notes"
            }
        }
        return Status.VERIFIED
    }
}
