package dev.juuri.noma.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class NotesDocumentIoTest {
    @Test fun exactLimitIsAccepted() {
        val bytes = ByteArray(32768) { (it % 251).toByte() }
        assertArrayEquals(bytes, NotesDocumentIo.readBounded(ByteArrayInputStream(bytes), bytes.size))
    }

    @Test fun oneExtraByteIsRejectedBeforeWritingPastLimit() {
        try {
            NotesDocumentIo.readBounded(ByteArrayInputStream(ByteArray(1025)), 1024)
            fail("Oversized file passed")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun emptyDocumentIsReturnedUnmodified() {
        assertArrayEquals(ByteArray(0), NotesDocumentIo.readBounded(ByteArrayInputStream(byteArrayOf()), 16))
    }

    @Test fun stalledProviderFailsInsteadOfLoopingForever() {
        val stalled = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int = 0
        }
        try {
            NotesDocumentIo.readBounded(stalled)
            fail("Stalled provider was accepted")
        } catch (_: IllegalStateException) { }
    }

    @Test fun zeroMaximumRejectsNonemptyDocument() {
        try {
            NotesDocumentIo.readBounded(ByteArrayInputStream(byteArrayOf(1)), 0)
            fail("Zero maximum was ignored")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun negativeMaximumRejectedBeforeReading() {
        var accessed = false
        val input = object : InputStream() {
            override fun read(): Int { accessed = true; return -1 }
        }
        try {
            NotesDocumentIo.readBounded(input, -1)
            fail("Negative maximum was ignored")
        } catch (_: IllegalArgumentException) { }
        assertFalse(accessed)
    }
}
