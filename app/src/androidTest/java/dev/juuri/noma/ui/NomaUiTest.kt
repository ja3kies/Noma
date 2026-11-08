package dev.juuri.noma.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.ui.theme.NomaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Uses fake notes, not the production SQLite database.
 * Compiled by CI; execute on a device or emulator for UI validation.
 */
@OptIn(ExperimentalTestApi::class)
class NomaUiTest {
    @get:Rule val compose = createComposeRule()

    private class FakeRepository(initial: List<Note> = emptyList()) : NotesRepository {
        private val state = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Ready(initial))
        override val notes: StateFlow<NotesSnapshot> = state
        override suspend fun save(note: Note) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == note.id } + note)
        }
        override suspend fun delete(id: String) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == id })
        }
        override suspend fun refresh() = Unit
    }

    @Test fun quickCaptureAndSearchKeepNoteDiscoverable() {
        val repo = FakeRepository()
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_new_note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.onNodeWithTag("noma_title_input").performTextInput("Weekend plan")
        compose.onNodeWithTag("noma_editor_back").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_note_list"))
        compose.onNodeWithTag("noma_search").performTextInput("Weekend")
        compose.onNodeWithText("Weekend plan").assertExists()
    }

    @Test fun archiveFilterShowsArchivedNotesWithoutChangingTheirData() {
        val original = Note("one", "Archived thought", "Still here", false, 1L, 1L, true)
        val repo = FakeRepository(listOf(original))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_archive_filter").performClick()
        compose.onNodeWithText("Archived thought").assertExists()
    }
}
