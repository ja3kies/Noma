package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class NoteSearchTest {
    private val note = Note(
        id = "a", title = "Weekend Trip", body = "Buy milk and a map",
        isPinned = true, createdAt = 1L, updatedAt = 1_770_000_000_000L,
        isArchived = false, tags = listOf("Travel", "home projects")
    )
    private fun find(q: String): Boolean = NoteSearch.parse(q, ZoneId.of("UTC")).matches(note)

    @Test fun emptyQueryMatchesAll() = assertTrue(find(""))
    @Test fun phraseMatchesAcrossSpaces() = assertTrue(find("\"Weekend Trip\""))
    @Test fun titleOnlyMatch() = assertTrue(find("title:WEEKEND"))
    @Test fun titleOnlyRejectsBodyMatch() = assertFalse(find("title:milk"))
    @Test fun bodyOnlyMatch() = assertTrue(find("body:milk"))
    @Test fun bodyOnlyRejectsTitleMatch() = assertFalse(find("body:Weekend"))
    @Test fun tagHashSyntaxMatches() = assertTrue(find("#travel"))
    @Test fun tagFieldSyntaxMatches() = assertTrue(find("tag:\"home projects\""))
    @Test fun negativeTermExcludesNote() = assertFalse(find("-milk"))
    @Test fun negativeTermCanBeAbsent() = assertTrue(find("-school"))
    @Test fun multipleTermsRequireAnd() = assertTrue(find("Weekend milk"))
    @Test fun multipleTermsFailIfOneMissing() = assertFalse(find("Weekend missing"))
    @Test fun pinnedFilterRecognized() = assertTrue(find("is:pinned"))
    @Test fun unpinnedFilterRecognized() = assertFalse(find("is:unpinned"))
    @Test fun tagPresenceRecognized() = assertTrue(find("has:tags"))
    @Test fun missingChecklistNotReturned() = assertFalse(find("has:checklist"))
    @Test fun scopeInAllRecognized() = assertEquals(SearchScope.ALL, NoteSearch.parse("in:all").scope)
    @Test fun archiveScopeRecognized() = assertEquals(SearchScope.ARCHIVED, NoteSearch.parse("is:archived").scope)
    @Test fun activeScopeRecognized() = assertEquals(SearchScope.ACTIVE, NoteSearch.parse("is:active").scope)
    @Test fun unknownOperatorFallsBackToLiteral() = assertFalse(find("state:maybe"))
    @Test fun malformedDatesDoNotCrash() = assertFalse(find("before:banana"))
    @Test fun quotedValuesAreSingleToken() = assertEquals(listOf("title:two words"), NoteSearch.tokenize("title:\"two words\""))
    @Test fun tokensRemainBounded() = assertEquals(NoteSearch.MAX_CLAUSES, NoteSearch.tokenize("x ".repeat(200)).size)
    @Test fun invalidUnclosedQuoteIsSafe() = assertTrue(find("\"Weekend Trip"))
    @Test fun dateBeforeAndAfterWork() {
        assertTrue(find("after:2026-01-01"))
        assertFalse(find("before:2025-01-01"))
    }
    @Test fun checklistFilterReturnsTasks() {
        val withTasks = note.copy(body = "- [ ] Work")
        assertTrue(NoteSearch.parse("has:checklist").matches(withTasks))
    }
    @Test fun negatedTagFilterWorks() = assertFalse(find("-#Travel"))
}
