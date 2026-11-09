package dev.juuri.noma.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class NotesBackupVerificationTest {
    @Test fun byteIdenticalBackupReportsVerifiedAndClosesOutput() {
        val bytes = "A UTF-8 note: café 🦊".toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()
        val status = NotesBackupVerification.writeAndVerify(
            bytes, { output }, { ByteArrayInputStream(output.toByteArray()) }
        )
        assertEquals(NotesBackupVerification.Status.VERIFIED, status)
        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test fun writeSuccessButUnreadableProviderIsNotCalledVerified() {
        val output = ByteArrayOutputStream()
        assertEquals(
            NotesBackupVerification.Status.UNVERIFIED_PROVIDER,
            NotesBackupVerification.writeAndVerify(
                "Noma".toByteArray(), { output }, { throw IOException("Access denied") }
            )
        )
        assertEquals("Noma", output.toString("UTF-8"))
    }

    @Test fun silentContentCorruptionIsRejected() {
        try {
            NotesBackupVerification.writeAndVerify(
                "correct".toByteArray(), { ByteArrayOutputStream() },
                { ByteArrayInputStream("corrupt".toByteArray()) }
            )
            fail("Corrupted backup wrongly verified")
        } catch (_: IllegalStateException) { }
    }

    @Test fun silentTruncationIsRejected() {
        try {
            NotesBackupVerification.writeAndVerify(
                "full backup".toByteArray(), { ByteArrayOutputStream() },
                { ByteArrayInputStream("full".toByteArray()) }
            )
            fail("Truncated backup wrongly verified")
        } catch (_: IllegalStateException) { }
    }

    @Test fun missingWriteDestinationFailsBeforeSuccess() {
        try {
            NotesBackupVerification.writeAndVerify("notes".toByteArray(), { null }, { null })
            fail("Missing document accepted")
        } catch (_: IllegalStateException) { }
    }

    @Test fun oversizedBackupIsRejectedBeforeOpeningProvider() {
        var opened = false
        try {
            NotesBackupVerification.writeAndVerify(
                ByteArray(NotesPortableFormat.MAX_IMPORT_BYTES + 1),
                { opened = true; ByteArrayOutputStream() },
                { null }
            )
            fail("Oversized backup accepted")
        } catch (_: IllegalArgumentException) { }
        assertFalse(opened)
    }
}
