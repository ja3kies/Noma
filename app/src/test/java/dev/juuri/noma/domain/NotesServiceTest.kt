package dev.juuri.noma.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotesServiceTest {
    private class MemoryRepository : NotesRepository {
        private val state = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Ready(emptyList()))
        override val notes: StateFlow<NotesSnapshot> = state
        override suspend fun importMissing(notes: List<Note>): Int {
            val old = (state.value as NotesSnapshot.Ready).entries
            val ids = old.mapTo(HashSet<String>()) { it.id }
            val missing = notes.filter { ids.add(it.id) }
            state.value = NotesSnapshot.Ready(old + missing)
            return missing.size
        }
        override suspend fun refresh() = Unit
        override suspend fun save(note: Note) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == note.id } + note)
        }
        override suspend fun delete(id: String) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == id })
        }
        fun list() = (state.value as NotesSnapshot.Ready).entries
    }

    private fun note(
        id: String, title: String = "", body: String = "", pinned: Boolean = false, time: Long = 1L
    ) = Note(id, title, body, pinned, time, time)

    @Test fun createsAndPersists() = runBlocking {
        val repo = MemoryRepository()
        val service = NotesService(repo, { 42L }, { "new-id" })
        val saved = service.create()
        assertEquals("new-id", saved.id)
        assertEquals(42L, saved.createdAt)
        assertEquals(saved, repo.list().single())
    }

    @Test fun updatesTimestampAndContent() = runBlocking {
        val repo = MemoryRepository()
        val service = NotesService(repo, { 100L })
        service.save(note("a", title = "draft", time = 1).copy(body = "edited"))
        assertEquals("edited", repo.list().single().body)
        assertEquals(100L, repo.list().single().updatedAt)
        assertEquals(1L, repo.list().single().createdAt)
    }

    @Test fun deleteAndRestoreKeepsOriginalData() = runBlocking {
        val repo = MemoryRepository()
        val service = NotesService(repo)
        val original = note("a", "Keep", pinned = true)
        repo.save(original)
        service.delete(original)
        assertTrue(repo.list().isEmpty())
        service.restore(original)
        assertEquals(original, repo.list().single())
    }

    @Test fun searchingMatchesTitleAndBodyCaseInsensitively() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(note("a", "Grocery LIST"), note("b", "Task", "Buy milk"))
        assertEquals(listOf("a"), service.visible(notes, " grocery ", false).map { it.id })
        assertEquals(listOf("b"), service.visible(notes, "MILK", false).map { it.id })
    }

    @Test fun pinnedAlwaysAppearBeforeRecentlyEdited() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(note("new", time = 100), note("pinned", pinned = true, time = 1))
        assertEquals(listOf("pinned", "new"), service.visible(notes, "", false).map { it.id })
        assertEquals(listOf("pinned"), service.visible(notes, "", true).map { it.id })
    }

    @Test fun emptyQueryListsAllAndUnknownQueryListsNone() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(note("a", "One"), note("b", "Two"))
        assertEquals(2, service.visible(notes, "   ", false).size)
        assertTrue(service.visible(notes, "unmatched", false).isEmpty())
    }

    @Test fun archiveVisibilityAndSearchAreScoped() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(
            note("active", "Shopping"),
            note("archived", "Private thoughts").copy(isArchived = true)
        )
        assertEquals(listOf("active"), service.visible(notes, "", false).map { it.id })
        assertEquals(listOf("archived"),
            service.visible(notes, "thoughts", false, archivedOnly = true).map { it.id })
        assertTrue(service.visible(notes, "thoughts", false).isEmpty())
    }

    @Test fun titleSortIsAlphabeticalWithinPinnedGroups() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(
            note("z", "Zebra", time = 100),
            note("a", "apple", time = 1),
            note("pZ", "Zoo", pinned = true, time = 80),
            note("pA", "Atlas", pinned = true, time = 10)
        )
        assertEquals(listOf("pA", "pZ", "a", "z"),
            service.visible(notes, "", false, sort = NoteSort.TITLE).map { it.id })
        assertEquals(listOf("pZ", "pA", "z", "a"),
            service.visible(notes, "", false).map { it.id })
    }

    @Test fun archivedNotesPersistAndRestoreWithoutLosingOtherFields() = runBlocking {
        val repo = MemoryRepository()
        val service = NotesService(repo, { 200L })
        val original = note("a", "A", pinned = true, time = 1L)
        repo.save(original)
        service.save(original.copy(isArchived = true))
        assertTrue(repo.list().single().isArchived)
        service.save(repo.list().single().copy(isArchived = false))
        assertFalse(repo.list().single().isArchived)
        assertTrue(repo.list().single().isPinned)
        assertEquals(1L, repo.list().single().createdAt)
    }

    @Test fun searchHandlesAccentsAndCaseWithoutMutatingNotes() {
        val entries = listOf(note("a", "Réunion à Nantes", "Préparer le café"))
        val service = NotesService(MemoryRepository())
        assertEquals(listOf("a"), service.visible(entries, "REUNION CAFE", false).map { it.id })
        assertEquals("Réunion à Nantes", entries.single().title)
    }

    @Test fun searchMatchesWordsAcrossTitleAndBodyInAnyOrder() {
        val entries = listOf(
            note("one", "Weekend plans", "Buy fresh croissants"),
            note("two", "Weekend plans", "Go swimming")
        )
        val service = NotesService(MemoryRepository())
        assertEquals(listOf("one"), service.visible(entries, "croissants weekend", false).map { it.id })
        assertEquals(listOf("one"), service.visible(entries, "  WEEKEND    fresh ", false).map { it.id })
    }

    @Test fun searchDoesNotMatchWhenAnyWordIsMissing() {
        val service = NotesService(MemoryRepository())
        val entries = listOf(note("one", "Project plan", "Research design references"))
        assertTrue(service.visible(entries, "project impossible", false).isEmpty())
    }

    @Test fun searchPreviewUsesMatchingBodyLine() {
        val entry = note("one", "A note", "First line\nMore context\nCafé at noon")
        assertEquals("Café at noon", NoteSearch.matchingPreview(entry, "cafe"))
        assertEquals("First line", NoteSearch.matchingPreview(entry, ""))
    }

    @Test fun searchPreviewFallsBackWhenOnlyTitleMatches() {
        val entry = note("one", "Project Atlas", "A first line\nAnother line")
        assertEquals("A first line", NoteSearch.matchingPreview(entry, "Atlas"))
    }

    @Test fun searchKeepsArchiveAndPinnedFilters() {
        val entries = listOf(
            note("active", "Été", "Projet", true),
            note("archived", "ETE", "Archive").copy(isArchived = true)
        )
        val service = NotesService(MemoryRepository())
        assertEquals(listOf("active"), service.visible(entries, "ete", true).map { it.id })
        assertEquals(listOf("archived"), service.visible(entries, "ete", false, archivedOnly = true).map { it.id })
    }

    @Test fun precomposedAndCombiningAccentsMatchEqually() {
        val service = NotesService(MemoryRepository())
        val entries = listOf(note("accent", "Cafe\u0301 matinal", "Texte"))
        assertEquals(listOf("accent"), service.visible(entries, "CAFÉ", false).map { it.id })
        assertEquals(listOf("accent"), service.visible(entries, "cafe", false).map { it.id })
    }

    @Test fun searchDoesNotAlterNoteBodiesOrTimestamps() {
        val service = NotesService(MemoryRepository())
        val original = note("id", "Réunion", "Note complète", pinned = true, time = 1234L)
        val before = original.copy()
        service.visible(listOf(original), "note reunion", false)
        assertEquals(before, original)
    }

    @Test fun leadingTrailingNewlinesAndWhitespaceAreIgnoredInQuery() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(note("id", "A weekend", "Go to the market"))
        assertEquals(listOf("id"), service.visible(notes, "\n  MARKET\tweekend  ", false).map { it.id })
    }

    @Test fun searchResultsStableForEqualTimestamps() {
        val service = NotesService(MemoryRepository())
        val notes = listOf(note("z", "Found", "match"), note("a", "Found", "match"))
        assertEquals(listOf("a", "z"), service.visible(notes, "match", false).map { it.id })
    }

    @Test fun contextualPreviewIncludesMatchAtEndOfLongLine() {
        val line = "Ordinary background. ".repeat(20) + "Important café rendezvous"
        val result = NoteSearch.matchingPreview(note("long", body = line), "CAFE")
        assertTrue(result.contains("café"))
        assertTrue(result.startsWith("…"))
        assertTrue(result.length <= 162)
    }

    @Test fun contextualPreviewPreservesAccentsAndSupplementaryCharacters() {
        val line = "🦊 ".repeat(100) + "Cafe\u0301 et réunion"
        val result = NoteSearch.matchingPreview(note("unicode", body = line), "reunion")
        assertTrue(result.contains("réunion"))
        assertTrue(result.length <= 162)
    }

    @Test fun matchingShortPreviewKeepsEntireLineWithoutEllipsis() {
        val body = "Préparer le café avant midi"
        assertEquals(body, NoteSearch.matchingPreview(note("short", body = body), "cafe"))
    }

    @Test fun titleMatchWithoutBodyTextReturnsEmptyPreview() {
        assertEquals("", NoteSearch.matchingPreview(note("title", title = "Réunion"), "REUNION"))
    }

    @Test fun veryLongTitleMatchDoesNotRequireMatchingBodyText() {
        val candidate = note("long", title = "Noma", body = "unrelated ".repeat(1000))
        assertTrue(NoteSearch.matches(candidate, "noma"))
    }

    @Test fun contextualExcerptPrioritizesWordsMissingFromTitle() {
        val candidate = note(
            "cross", title = "Réunion d'équipe",
            body = "Réunion avec le groupe\nDocuments à préparer et café à acheter"
        )
        assertEquals(
            "Documents à préparer et café à acheter",
            NoteSearch.matchingPreview(candidate, "reunion cafe")
        )
    }

    @Test fun contextualExcerptPrefersLineCoveringMostSearchTerms() {
        val candidate = note(
            "score", title = "Courses",
            body = "Acheter le café\nAcheter le café et le lait\nPrendre le lait"
        )
        assertEquals(
            "Acheter le café et le lait",
            NoteSearch.matchingPreview(candidate, "cafe lait")
        )
    }

    @Test fun titleOnlyMatchFallsBackToFirstNonemptyBodyLine() {
        val candidate = note("first", title = "Essai", body = "\n Premier paragraphe\nSuite")
        assertEquals("Premier paragraphe", NoteSearch.matchingPreview(candidate, "essai"))
    }

    @Test fun notePresentationFallbackUsesBody() {
        val entry = note("a", body = "\n  A line about today\nSomething else")
        assertEquals("A line about today", entry.displayTitle)
        assertEquals("A line about today", entry.preview)
    }
}
