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
