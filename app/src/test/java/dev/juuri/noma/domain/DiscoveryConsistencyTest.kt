package dev.juuri.noma.domain

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

/** Separate behavioral gates for discovery and sorting after performance refactors. */
class DiscoveryConsistencyTest {
    private val repository = object : NotesRepository {
        override val notes = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Ready(emptyList()))
        override suspend fun refresh() = Unit
        override suspend fun save(note: Note) = Unit
        override suspend fun delete(id: String) = Unit
        override suspend fun importMissing(notes: List<Note>): Int = 0
    }
    private val service = NotesService(repository)
    private fun note(
        id: String, title: String = id, body: String = "",
        pin: Boolean = false, archive: Boolean = false, time: Long = 1L
    ) = Note(id, title, body, pin, 1L, time, archive)
    private fun ids(notes: List<Note>, q: String = "", pin: Boolean = false,
        archive: Boolean = false, sort: NoteSort = NoteSort.RECENT) =
        service.visible(notes, q, pin, archive, sort).map { it.id }

    @Test fun emptyNotebookSearchIsEmpty() {
        assertTrue(ids(emptyList(), "query").isEmpty())
    }
    @Test fun emptyNotebookSortedIsEmpty() {
        assertTrue(ids(emptyList(), sort = NoteSort.TITLE).isEmpty())
    }
    @Test fun defaultExcludesArchived() {
        assertEquals(listOf("a"), ids(listOf(note("a"), note("b", archive = true))))
    }
    @Test fun archiveFilterExcludesActive() {
        assertEquals(listOf("b"), ids(listOf(note("a"), note("b", archive = true)), archive = true))
    }
    @Test fun pinnedOnlyExcludesUnpinned() {
        assertEquals(listOf("p"), ids(listOf(note("a"), note("p", pin = true)), pin = true))
    }
    @Test fun pinnedOnlyWithinArchivesWorks() {
        val notes = listOf(note("p", pin = true, archive = true), note("a", archive = true))
        assertEquals(listOf("p"), ids(notes, pin = true, archive = true))
    }
    @Test fun pinnedGroupBeforeRecentNotes() {
        val notes = listOf(note("latest", time = 30), note("pinned", pin = true, time = 1))
        assertEquals(listOf("pinned", "latest"), ids(notes))
    }
    @Test fun recentSortWithinPinnedGroup() {
        val notes = listOf(note("older", pin = true, time = 1), note("newer", pin = true, time = 2))
        assertEquals(listOf("newer", "older"), ids(notes))
    }
    @Test fun recentSortWithinUnpinnedGroup() {
        val notes = listOf(note("older", time = 1), note("newer", time = 2))
        assertEquals(listOf("newer", "older"), ids(notes))
    }
    @Test fun titleSortIgnoresAccentDifferences() {
        val notes = listOf(note("z", "Zèbre"), note("a", "Âne"))
        assertEquals(listOf("a", "z"), ids(notes, sort = NoteSort.TITLE))
    }
    @Test fun titleSortRetainsPinnedPriority() {
        val notes = listOf(note("a", "A"), note("z", "Z", pin = true))
        assertEquals(listOf("z", "a"), ids(notes, sort = NoteSort.TITLE))
    }
    @Test fun recentTieBreakById() {
        val notes = listOf(note("z", time = 2), note("a", time = 2))
        assertEquals(listOf("a", "z"), ids(notes))
    }
    @Test fun titleTieBreakByRecentTime() {
        val notes = listOf(note("old", "Cafe", time = 1), note("new", "Café", time = 3))
        assertEquals(listOf("new", "old"), ids(notes, sort = NoteSort.TITLE))
    }
    @Test fun multiWordSearchAcrossTitleAndBody() {
        val notes = listOf(note("a", "Café", "Œuvre"), note("b", "Café", "Book"))
        assertEquals(listOf("a"), ids(notes, "oeuvre cafe"))
    }
    @Test fun searchExcludesPartialMatches() {
        assertTrue(ids(listOf(note("a", "Café", "Book")), "cafe oeuvre").isEmpty())
    }
    @Test fun exactTitleOutranksContainsTitle() {
        val notes = listOf(note("partial", "Café de Paris"), note("exact", "Café"))
        assertEquals(listOf("exact", "partial"), ids(notes, "cafe"))
    }
    @Test fun titlePrefixOutranksBodyMatch() {
        val notes = listOf(note("body", "Other", "Café"), note("prefix", "Café de Paris"))
        assertEquals(listOf("prefix", "body"), ids(notes, "cafe"))
    }
    @Test fun searchDoesNotMutateOriginalList() {
        val notes = listOf(note("b", "Café", time = 4), note("a", "Tea", time = 1))
        val snapshot = notes.toList()
        ids(notes, "cafe")
        assertEquals(snapshot, notes)
    }
    @Test fun sortDoesNotMutateOriginalList() {
        val notes = listOf(note("z", "Z"), note("a", "A"))
        ids(notes, sort = NoteSort.TITLE)
        assertEquals(listOf("z", "a"), notes.map { it.id })
    }
    @Test fun filteredSearchRespectsArchiveScope() {
        val notes = listOf(note("active", "Café"), note("archived", "Café", archive = true))
        assertEquals(listOf("active"), ids(notes, "cafe"))
        assertEquals(listOf("archived"), ids(notes, "cafe", archive = true))
    }
}
