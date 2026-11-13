package dev.juuri.noma.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindNavigationTest {
    @Test fun nextWrapsFromLastOccurrenceToFirst() {
        assertEquals(4..6, NoteFindReplace.nextMatch("cat CAT", "cat", 3))
        assertEquals(0..2, NoteFindReplace.nextMatch("cat CAT", "cat", 7))
    }
    @Test fun previousWrapsAndSkipsCurrentOccurrence() {
        assertEquals(4..6, NoteFindReplace.nextMatch("cat CAT", "cat", -1, true))
        assertEquals(0..2, NoteFindReplace.nextMatch("cat CAT", "cat", 3, true))
    }
    @Test fun absentOrBlankQueryHasNoSelection() {
        assertNull(NoteFindReplace.nextMatch("text", " ", 0))
        assertNull(NoteFindReplace.nextMatch("text", "other", 0))
        assertNull(NoteFindReplace.nextMatch("", "other", 0, true))
    }
    @Test fun emojiOffsetsRemainOriginalUtf16Coordinates() {
        assertEquals(3..6, NoteFindReplace.nextMatch("🌿 café", "CAFÉ", 0))
    }
    @Test fun metacharactersAreLiteral() {
        assertEquals(2..2, NoteFindReplace.nextMatch("a.b", "b", 0))
        assertEquals(1..1, NoteFindReplace.nextMatch("a.b", ".", 0))
    }
}
