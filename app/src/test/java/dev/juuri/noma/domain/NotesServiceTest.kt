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

    @Test fun notePresentationFallbackUsesBody() {
        val entry = note("a", body = "\n  A line about today\nSomething else")
        assertEquals("A line about today", entry.displayTitle)
        assertEquals("A line about today", entry.preview)
    }
}
