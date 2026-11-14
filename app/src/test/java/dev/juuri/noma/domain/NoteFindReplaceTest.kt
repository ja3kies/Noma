package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

class NoteFindReplaceTest {
    @Test fun emptySearchCountsZero() {
        assertEquals(0, NoteFindReplace.count("hello", ""))
    }
    @Test fun whitespaceSearchDoesNotReplaceDocument() {
        assertEquals("x y", NoteFindReplace.replaceAll("x y", " ", "z"))
    }
    @Test fun countIsCaseInsensitive() {
        assertEquals(3, NoteFindReplace.count("Note note NOTE", "note"))
    }
    @Test fun countUsesNonOverlappingMatches() {
        assertEquals(2, NoteFindReplace.count("aaaa", "aa"))
    }
    @Test fun forwardNavigationSkipsOverlapsAndWraps() {
        assertEquals(0..1, NoteFindReplace.nextMatch("aaaa", "aa", 0))
        assertEquals(2..3, NoteFindReplace.nextMatch("aaaa", "aa", 1))
        assertEquals(0..1, NoteFindReplace.nextMatch("aaaa", "aa", 4))
    }
    @Test fun backwardNavigationSkipsOverlapsAndWraps() {
        assertEquals(0..1, NoteFindReplace.nextMatch("aaaa", "aa", 1, backwards = true))
        assertEquals(2..3, NoteFindReplace.nextMatch("aaaa", "aa", -1, backwards = true))
        assertEquals(2..3, NoteFindReplace.nextMatch("aaaa", "aa", 3, backwards = true))
    }
    @Test fun navigationRespectsCaseAndNonOverlappingCounts() {
        assertEquals(5..8, NoteFindReplace.nextMatch("Note note NOTE", "note", 4))
        assertEquals(10..13, NoteFindReplace.nextMatch("Note note NOTE", "note", 9))
        assertEquals(3, NoteFindReplace.count("Note note NOTE", "note"))
        assertNull(NoteFindReplace.nextMatch("text", "missing", 0))
        assertNull(NoteFindReplace.nextMatch("text", " ", 0))
    }
    @Test fun ordinalUsesOnlyCanonicalNonOverlappingMatchStarts() {
        assertEquals(1, NoteFindReplace.matchOrdinal("aaaa", "aa", 0))
        assertEquals(0, NoteFindReplace.matchOrdinal("aaaa", "aa", 1))
        assertEquals(2, NoteFindReplace.matchOrdinal("aaaa", "aa", 2))
        assertEquals(0, NoteFindReplace.matchOrdinal("aaaa", "aa", 3))
        assertEquals(0, NoteFindReplace.matchOrdinal("aaaa", "aa", -1))
        assertEquals(0, NoteFindReplace.matchOrdinal("aaaa", "", 0))
        assertEquals(3, NoteFindReplace.matchOrdinal("Note note NOTE", "note", 10))
    }

    @Test fun replaceAtOrNextTargetsCurrentMatchInsteadOfAlwaysFirst() {
        assertEquals("cat one X", NoteFindReplace.replaceAtOrNext("cat one cat", "cat", "X", 8))
        assertEquals("X one cat", NoteFindReplace.replaceAtOrNext("cat one cat", "cat", "X", 0))
    }
    @Test fun replaceAtOrNextWrapsAndUsesNonOverlappingRanges() {
        assertEquals("aaX", NoteFindReplace.replaceAtOrNext("aaaa", "aa", "X", 1))
        assertEquals("Xaa", NoteFindReplace.replaceAtOrNext("aaaa", "aa", "X", 4))
        assertEquals("😀 x", NoteFindReplace.replaceAtOrNext("😀 😀", "😀", "x", 3))
    }
    @Test fun replaceAtOrNextNoMatchPreservesText() {
        assertEquals("hello", NoteFindReplace.replaceAtOrNext("hello", "xyz", "X", 0))
        assertEquals("hello", NoteFindReplace.replaceAtOrNext("hello", "", "X", 0))
    }

    @Test fun replaceFirstChangesOnlyFirstMatch() {
        assertEquals("New old OLD", NoteFindReplace.replaceFirst("old old OLD", "old", "New"))
    }
    @Test fun replaceAllChangesEveryCaseVariant() {
        assertEquals("X X X", NoteFindReplace.replaceAll("a A a", "a", "X"))
    }
    @Test fun missingSearchReturnsSameBody() {
        assertEquals("original", NoteFindReplace.replaceAll("original", "missing", "new"))
    }
    @Test fun replaceWithEmptyStringDeletesMatches() {
        assertEquals("a b", NoteFindReplace.replaceAll("a xxx b xxx", " xxx", ""))
    }
    @Test fun replacementIsLiteralNotRegex() {
        assertEquals("a$\\.b", NoteFindReplace.replaceAll("a.c", ".c", "$\\.b"))
    }
    @Test fun emojiTextRemainsIntact() {
        assertEquals("🦊 NEW 🦊", NoteFindReplace.replaceAll("🦊 café 🦊", "CAFÉ", "NEW"))
    }
    @Test fun overlappingNeedleDoesNotLoop() {
        assertEquals("bb", NoteFindReplace.replaceAll("aaaa", "aa", "b"))
    }
    @Test fun replacementContainingNeedleDoesNotLoop() {
        assertEquals("aaa", NoteFindReplace.replaceAll("a", "a", "aaa"))
    }
    @Test fun noOpReplacementReturnsOriginalText() {
        assertEquals("Hello", NoteFindReplace.replaceFirst("Hello", "hello", "Hello"))
    }
    @Test fun longBodyReplacementStaysBoundedAndCorrect() {
        val body = "word ".repeat(10_000)
        val output = NoteFindReplace.replaceAll(body, "word", "note")
        assertEquals(body.length, output.length)
        assertEquals(10_000, NoteFindReplace.count(output, "note"))
    }
}
