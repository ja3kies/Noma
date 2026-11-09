package dev.juuri.noma.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import dev.juuri.noma.domain.Note
import dev.juuri.noma.capture.CaptureRequest
import dev.juuri.noma.capture.NomaWidgetStyle
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.ui.theme.NomaTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Uses fake notes, not the production SQLite database.
 * Compiled by CI; execute on a device or emulator for UI validation.
 */
@OptIn(ExperimentalTestApi::class)
class NomaUiTest {
    @get:Rule val compose = createComposeRule()

    private class FakeRepository(
        initial: List<Note> = emptyList(),
        initialState: NotesSnapshot = NotesSnapshot.Ready(initial)
    ) : NotesRepository {
        private val state = MutableStateFlow<NotesSnapshot>(initialState)
        override val notes: StateFlow<NotesSnapshot> = state
        var saveGate: CompletableDeferred<Unit>? = null
        override suspend fun save(note: Note) {
            saveGate?.await()
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == note.id } + note)
        }
        override suspend fun delete(id: String) {
            val old = (state.value as NotesSnapshot.Ready).entries
            state.value = NotesSnapshot.Ready(old.filterNot { it.id == id })
        }
        override suspend fun importMissing(notes: List<Note>): Int {
            val old = (state.value as NotesSnapshot.Ready).entries
            val ids = old.mapTo(HashSet<String>()) { it.id }
            val missing = notes.filter { ids.add(it.id) }
            state.value = NotesSnapshot.Ready(old + missing)
            return missing.size
        }
        override suspend fun refresh() = Unit
    }

    @Test fun backupCheckRemainsAvailableAfterLocalStorageFailure() {
        compose.setContent {
            NomaTheme {
                PortableDocuments(
                    snapshot = NotesSnapshot.Error("Local database unreadable"),
                    onImport = { 0 },
                    snapshotForExport = { emptyList() },
                    onMessage = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Import and export notes").performClick()
        compose.onNodeWithText("Check backup file").assertExists()
    }

    @Test fun repositoryFailureDoesNotOfferBrokenCaptureButton() {
        val repo = FakeRepository(initialState = NotesSnapshot.Error("unreadable"))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_new_note").assertDoesNotExist()
        compose.onNodeWithTag("noma_search").assertIsNotEnabled()
        compose.onNodeWithText("Try again").assertExists()
        compose.onNodeWithContentDescription("Import and export notes").assertExists()
    }

    @Test fun pendingRepositoryStillDisablesDiscoveryAndCapture() {
        val repo = FakeRepository(initialState = NotesSnapshot.Loading)
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_new_note").assertDoesNotExist()
        compose.onNodeWithTag("noma_search").assertIsNotEnabled()
    }

    @Test fun noteActionsIdentifyTheirNoteForTalkBack() {
        val repo = FakeRepository(listOf(Note(
            "one", "Réunion", "Agenda", false, 1L, 2L
        )))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithContentDescription("Actions for Réunion").assertExists()
    }

    @Test fun archivedActionsDoNotShowPinThatCannotAffectTheArchive() {
        val repo = FakeRepository(listOf(Note(
            "archived", "Finished task", "Done", false, 1L, 2L, true
        )))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_archive_filter").performClick()
        compose.onNodeWithContentDescription("Actions for Finished task").performClick()
        compose.onNodeWithText("Pin note").assertDoesNotExist()
        compose.onNodeWithText("Restore note").assertExists()
    }

    @Test fun sharedTextPreviewDisclosesTruncationWithoutTruncatingSavedContents() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        val fullText = "A".repeat(750)
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("Long", fullText))
        }
        compose.onNodeWithText("Preview shows the first 500 of 750 characters. The full text will be saved.")
            .assertExists()
        compose.onNodeWithText("Save note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.runOnIdle {
            val entries = (repo.notes.value as NotesSnapshot.Ready).entries
            assertEquals(fullText, entries.single().body)
        }
    }

    @Test fun quickCaptureAndSearchKeepNoteDiscoverable() {
        val repo = FakeRepository()
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        // Empty notebook offers the centered action, not a duplicate FAB.
        compose.onNodeWithTag("noma_new_note").assertDoesNotExist()
        compose.onNodeWithText("New note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.onNodeWithTag("noma_title_input").performTextInput("Weekend plan")
        compose.onNodeWithTag("noma_editor_back").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_note_list"))
        compose.onNodeWithTag("noma_search").performTextInput("Weekend")
        compose.onNodeWithText("Weekend plan").assertExists()
    }

    @Test fun slowNoteCreationShowsProgressAndBlocksRepeatedTaps() {
        val repo = FakeRepository()
        val gate = CompletableDeferred<Unit>()
        repo.saveGate = gate
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("New note").performClick()
        compose.onNodeWithText("Creating note…").assertExists().assertIsNotEnabled()
        compose.runOnIdle { assertTrue((repo.notes.value as NotesSnapshot.Ready).entries.isEmpty()) }
        compose.runOnIdle { gate.complete(Unit) }
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.runOnIdle {
            assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        }
    }

    @Test fun populatedNotebookRetainsTheFastCreationFab() {
        val repo = FakeRepository(listOf(
            Note("existing", "Present note", "Body", false, 1L, 2L)
        ))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_new_note").assertExists()
    }

    @Test fun sectionTitleFollowsSortAndSearchInsteadOfClaimingEverythingIsRecent() {
        val repo = FakeRepository(listOf(
            Note("a", "Café", "Bring coffee", false, 1L, 2L),
            Note("b", "Agenda", "Plan", false, 2L, 3L)
        ))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithText("Recent").assertExists()
        compose.onNodeWithContentDescription("Sort notes").performClick()
        compose.onNodeWithText("Title (A–Z)").performClick()
        compose.onNodeWithText("A–Z").assertExists()
        compose.onNodeWithTag("noma_search").performTextInput("Cafe")
        compose.onNodeWithText("Search results").assertExists()
        compose.onNodeWithText("Café").assertExists()
    }

    @Test fun deleteDialogNamesTheExactNoteAndCancelPreservesIt() {
        val original = Note("one", "Work item", "Keep this", false, 1L, 1L)
        val repo = FakeRepository(listOf(original))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithContentDescription("Actions for Work item").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete “Work item”? You can undo this action briefly.").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Work item").assertExists()
        compose.runOnIdle {
            assertEquals(listOf(original), (repo.notes.value as NotesSnapshot.Ready).entries)
        }
    }

    @Test fun editorMenuDoesNotRepeatThePrimaryPinAction() {
        val repo = FakeRepository(listOf(
            Note("one", "Open note", "Body", false, 1L, 1L)
        ))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Open note").performClick()
        compose.onNodeWithContentDescription("Note actions").performClick()
        compose.onNodeWithText("Pin note").assertDoesNotExist()
        compose.onNodeWithText("Archive note").assertExists()
    }

    @Test fun clearingSearchFiltersRestoresAllNotes() {
        val original = Note("one", "Réunion", "Important content", false, 1L, 1L)
        val repo = FakeRepository(listOf(original))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_search").performTextInput("not found")
        compose.onNodeWithTag("noma_clear_filters").performClick()
        compose.onNodeWithText("Réunion").assertExists()
    }

    @Test fun accentInsensitiveSearchShowsMatchingNoteAndRelevantBodyLine() {
        val note = Note(
            "accent", "Réunion", "Un premier paragraphe\nPréparer le café avec Nadia",
            false, 1L, 2L
        )
        val repo = FakeRepository(listOf(note))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_search").performTextInput("reunion cafe")
        compose.onNodeWithText("Réunion").assertExists()
        compose.onNodeWithText("Préparer le café avec Nadia").assertExists()
        compose.onNodeWithText("1 result").assertExists()
    }

    @Test fun newNoteDuringSearchIsVisibleAfterReturningToNotebook() {
        val repo = FakeRepository(listOf(
            Note("existing", "Previous", "Contains café", false, 1L, 2L)
        ))
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithTag("noma_search").performTextInput("unknown")
        compose.onNodeWithTag("noma_new_note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.onNodeWithTag("noma_title_input").performTextInput("A new thought")
        compose.onNodeWithTag("noma_editor_back").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_note_list"))
        compose.onNodeWithText("A new thought").assertExists()
    }

    @Test fun searchFieldIsExplicitlyDescribedForScreenReaders() {
        val repo = FakeRepository()
        val viewModel = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(viewModel) } }
        compose.onNodeWithContentDescription("Search your notes").assertExists()
    }

    @Test fun shareSheetCaptureShowsPreviewAndDoesNotSaveUntilConfirmed() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.runOnIdle {
            model.receiveSharedText(
                CaptureRequest.SharedText("Article title", "https://example.org/idea")
            )
        }
        compose.onNodeWithText("Save shared text?").assertExists()
        compose.onNodeWithText("Article title").assertExists()
        compose.onNodeWithText("https://example.org/idea").assertExists()
        compose.runOnIdle {
            assertTrue((repo.notes.value as NotesSnapshot.Ready).entries.isEmpty())
        }
        compose.onNodeWithText("Save note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.runOnIdle {
            val stored = (repo.notes.value as NotesSnapshot.Ready).entries
            assertEquals(1, stored.size)
            assertEquals("https://example.org/idea", stored.single().body)
            assertEquals("Article title", stored.single().title)
        }
    }

    @Test fun dismissedShareNeverWritesToRepository() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("", "Private data"))
        }
        compose.onNodeWithText("Discard").performClick()
        compose.runOnIdle {
            assertTrue((repo.notes.value as NotesSnapshot.Ready).entries.isEmpty())
            assertEquals(null, model.pendingShare.value)
        }
    }

    @Test fun appearanceSettingsExposeThreeWidgetStyles() {
        val repo = FakeRepository()
        val viewModel = NotesViewModel(repo, NotesService(repo))
        var chosen = NomaWidgetStyle.MATERIAL_YOU
        compose.setContent {
            NomaTheme {
                NomaApp(
                    viewModel = viewModel,
                    widgetStyle = chosen,
                    onWidgetStyleChange = { chosen = it }
                )
            }
        }
        compose.onNodeWithContentDescription("Appearance").performClick()
        compose.onNodeWithText("Widget appearance").performClick()
        compose.onNodeWithText("Material You").assertExists()
        compose.onNodeWithText("Frosted green").assertExists()
        compose.onNodeWithText("Liquid Glass").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(NomaWidgetStyle.LIQUID_GLASS, chosen) }
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
