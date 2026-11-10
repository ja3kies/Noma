package dev.juuri.noma.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onRoot
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
import dev.juuri.noma.ui.theme.NomaAppearance
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
        fun updateSnapshot(snapshot: NotesSnapshot) { state.value = snapshot }
        var saveGate: CompletableDeferred<Unit>? = null
        @Volatile var failWrites = false
        @Volatile var saveAttempts = 0
        override suspend fun save(note: Note) {
            saveAttempts++
            saveGate?.await()
            if (failWrites) throw IllegalStateException("Simulated disk failure")
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
        compose.onNodeWithText("Noma could not read your notes. Try again. If the problem persists, verify a backup before making changes.").assertExists()
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
        // One persistent creation action serves empty and populated notebooks.
        compose.onNodeWithTag("noma_new_note").assertExists().performClick()
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
        compose.onNodeWithTag("noma_new_note").performClick()
        compose.waitUntil(5_000) { model.creating.value }
        compose.onNodeWithTag("noma_creation_spinner", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("noma_new_note").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Creating note…").assertExists()
        // A repeated request while the first insert is blocked must be ignored.
        compose.runOnIdle {
            model.createNote()
            assertTrue(model.creating.value)
            assertTrue((repo.notes.value as NotesSnapshot.Ready).entries.isEmpty())
        }
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

    @Test fun sortButtonAnnouncesTheActualSortMode() {
        val repo = FakeRepository(listOf(Note("sort", "One", "Body", false, 1L, 1L)))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_sort_button").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Recently edited")
        )
        compose.onNodeWithTag("noma_sort_button").performClick()
        compose.onNodeWithText("Title (A–Z)").performClick()
        compose.onNodeWithTag("noma_sort_button").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Title (A–Z)")
        )
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


    @Test fun cardActionsOpenWithoutSelectingTheNote() {
        val note = Note("menu-card", "Menu note", "Safe contents", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_note_actions_menu-card").performClick()
        compose.onNodeWithText("Archive note").assertExists()
        compose.runOnIdle { assertEquals(null, model.active.value) }
    }

    @Test fun emptyNotebookOffersSingleActionEvenWithActiveSearchFilter() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("nothing matches")
        compose.onNodeWithTag("noma_new_note").assertExists()
        compose.onNodeWithTag("noma_clear_filters").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { model.query.value.isEmpty() }
        compose.onNodeWithTag("noma_clear_filters").assertDoesNotExist()
        compose.onNodeWithTag("noma_new_note").assertExists()
    }

    @Test fun sharingAnEmptyNoteIsDisabledInEditor() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_new_note").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_editor_actions"))
        compose.onNodeWithTag("noma_editor_actions").performClick()
        compose.onNodeWithText("Share note").assertExists().assertIsNotEnabled()
    }


    @Test fun sharedCaptureExplainsWhyCurrentSavingDraftMustFinish() {
        val old = Note("share-wait", "Existing thought", "Stored", false, 1L, 1L)
        val repo = FakeRepository(listOf(old))
        val gate = CompletableDeferred<Unit>()
        repo.saveGate = gate
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Existing thought").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" new draft")
        compose.waitUntil(5_000) { model.saveState.value == EditorSaveState.Saving }
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("A link", "https://example.org"))
        }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("noma_share_save_guidance").assertExists()
        compose.runOnIdle { gate.complete(Unit) }
        compose.waitUntil(5_000) { model.saveState.value == EditorSaveState.Saved }
        compose.onNodeWithTag("noma_share_confirm").assertIsEnabled()
        compose.onNodeWithTag("noma_share_save_guidance").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("share-wait", model.active.value?.id)
        }
    }

    @Test fun sharedCaptureCannotBeConfirmedAfterFailedEditorWrite() {
        val old = Note("share-failed", "Not saved note", "Stored", false, 1L, 1L)
        val repo = FakeRepository(listOf(old))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Not saved note").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" new draft")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("A link", "https://example.org"))
        }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("noma_share_save_guidance").assertExists()
        compose.runOnIdle {
            assertEquals("share-failed", model.active.value?.id)
            assertEquals("https://example.org", model.pendingShare.value?.body)
            assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        }
    }

    @Test fun sharedCaptureCanBeConfirmedForAlreadySavedEditor() {
        val old = Note("share-ready", "Saved note", "Stored", false, 1L, 1L)
        val repo = FakeRepository(listOf(old))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Saved note").performClick()
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("A link", "https://example.org"))
        }
        compose.onNodeWithTag("noma_share_confirm").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { model.pendingShare.value == null }
        compose.runOnIdle {
            assertEquals("https://example.org", model.active.value?.body)
            assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        }
    }

    @Test fun sharedCaptureCancelLeavesCurrentFailedDraftUntouched() {
        val old = Note("share-cancel", "Draft in progress", "Stored", false, 1L, 1L)
        val repo = FakeRepository(listOf(old))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Draft in progress").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" failed")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        compose.runOnIdle {
            model.receiveSharedText(CaptureRequest.SharedText("Temporary", "Do not save"))
        }
        compose.onNodeWithText("Discard").performClick()
        compose.runOnIdle {
            assertEquals(null, model.pendingShare.value)
            assertEquals("share-cancel", model.active.value?.id)
            assertTrue(model.active.value?.body?.contains("failed") == true)
            assertTrue(model.saveState.value is EditorSaveState.Failed)
            assertEquals(1, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        }
    }

    @Test fun archivedNoteCannotBePinnedFromEditorToolbar() {
        val note = Note("archive", "Archived text", "Saved", false, 1L, 1L, true)
        val repo = FakeRepository(listOf(note))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_archive_filter").performClick()
        compose.onNodeWithText("Archived text").performClick()
        compose.onNodeWithTag("noma_editor_pin").assertIsNotEnabled()
    }

    @Test fun savingNoteDisablesEditorPinArchiveDeleteAndShare() {
        val note = Note("pending", "Working draft", "Initial", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        val gate = CompletableDeferred<Unit>()
        repo.saveGate = gate
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Working draft").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" pending")
        compose.runOnIdle { assertEquals(EditorSaveState.Saving, model.saveState.value) }
        compose.onNodeWithTag("noma_editor_pin").assertIsNotEnabled()
        compose.onNodeWithTag("noma_editor_actions").performClick()
        compose.onNodeWithText("Archive note").assertIsNotEnabled()
        compose.onNodeWithText("Delete").assertIsNotEnabled()
        compose.onNodeWithText("Share note").assertIsNotEnabled()
        compose.runOnIdle { gate.complete(Unit) }
        compose.waitUntil(5_000) { model.saveState.value == EditorSaveState.Saved }
    }

    @Test fun failedSaveKeepsEditorTextAndSupportsRetry() {
        val note = Note("failed", "Original", "First draft", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Original").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" updated")
        try {
            compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        } catch (failure: Throwable) {
            throw AssertionError(
                "Initial write failure not shown: state=${model.saveState.value}, attempts=${repo.saveAttempts}",
                failure
            )
        }
        compose.onNodeWithTag("noma_retry_save").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("noma_save_failure_guidance").assertExists()
        val attemptsBeforeRetry = repo.saveAttempts
        compose.runOnIdle {
            assertTrue(model.active.value?.body?.contains("updated") == true)
            repo.failWrites = false
        }
        compose.onNodeWithTag("noma_retry_save").performClick()
        try {
            compose.waitUntil(10_000) {
                repo.saveAttempts > attemptsBeforeRetry &&
                    model.saveState.value == EditorSaveState.Saved
            }
        } catch (failure: Throwable) {
            throw AssertionError(
                "Retry did not confirm persistence: state=${model.saveState.value}, " +
                    "attempts=${repo.saveAttempts}, prior=$attemptsBeforeRetry, " +
                    "body=${(repo.notes.value as NotesSnapshot.Ready).entries.singleOrNull()?.body}",
                failure
            )
        }
        compose.runOnIdle {
            assertTrue(
                (repo.notes.value as NotesSnapshot.Ready).entries.single().body.contains("updated")
            )
        }
    }


    @Test fun failedRetryKeepsDraftAndAllowsAnotherAttempt() {
        val note = Note("retry-again", "Retry note", "Before", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Retry note").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" added")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        val before = repo.saveAttempts
        compose.onNodeWithTag("noma_retry_save").performClick()
        compose.waitUntil(10_000) {
            repo.saveAttempts > before && model.saveState.value is EditorSaveState.Failed
        }
        compose.waitForIdle() // The same failure text can be conflated across rapid retries.
        compose.onNodeWithTag("noma_retry_save").assertExists().assertIsDisplayed()
        val secondBefore = repo.saveAttempts
        compose.runOnIdle {
            assertTrue(model.active.value?.body?.contains("added") == true)
            assertTrue(model.saveState.value is EditorSaveState.Failed)
            repo.failWrites = false
        }
        compose.onNodeWithTag("noma_retry_save").performClick()
        try {
            compose.waitUntil(10_000) { repo.saveAttempts > secondBefore }
        } catch (failure: Throwable) {
            throw AssertionError(
                "Second Retry click did not start a write; attempts=${repo.saveAttempts}, " +
                    "before=$secondBefore, state=${model.saveState.value}", failure
            )
        }
        try {
            compose.waitUntil(10_000) { model.saveState.value == EditorSaveState.Saved }
        } catch (failure: Throwable) {
            throw AssertionError(
                "Second Retry write did not commit; attempts=${repo.saveAttempts}, " +
                    "state=${model.saveState.value}, disk=${(repo.notes.value as NotesSnapshot.Ready).entries.single().body}",
                failure
            )
        }
        compose.runOnIdle {
            assertTrue((repo.notes.value as NotesSnapshot.Ready).entries.single().body.contains("added"))
        }
    }

    @Test fun failedSaveBackDialogKeepEditingPreservesDraft() {
        val note = Note("keep", "Keep editor", "Before", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Keep editor").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" preserved")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        compose.onNodeWithTag("noma_editor_back").performClick()
        compose.onNodeWithText("Changes not saved").assertExists()
        compose.onNodeWithTag("noma_keep_editing").performClick()
        compose.runOnIdle {
            assertEquals("keep", model.active.value?.id)
            assertTrue(model.active.value?.body?.contains("preserved") == true)
            assertTrue(model.saveState.value is EditorSaveState.Failed)
        }
    }

    @Test fun explicitDiscardAfterFailedSaveLeavesPersistedVersionIntact() {
        val note = Note("discard", "Discard draft", "Persisted body", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Discard draft").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" unsaved")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        compose.onNodeWithTag("noma_editor_back").performClick()
        compose.onNodeWithTag("noma_discard_unsaved").performClick()
        compose.runOnIdle {
            assertEquals(null, model.active.value)
            assertEquals(note, (repo.notes.value as NotesSnapshot.Ready).entries.single())
        }
    }

    @Test fun retryWithSlowDiskDisablesConflictingActionsUntilCommit() {
        val note = Note("blocked", "Retry pending", "Old", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Retry pending").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" new")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        val gate = CompletableDeferred<Unit>()
        compose.runOnIdle {
            repo.failWrites = false
            repo.saveGate = gate
        }
        compose.onNodeWithTag("noma_retry_save").performClick()
        compose.waitUntil(10_000) { model.saveState.value == EditorSaveState.Saving }
        compose.onNodeWithTag("noma_retry_save").assertDoesNotExist()
        compose.onNodeWithTag("noma_editor_pin").assertIsNotEnabled()
        compose.runOnIdle { gate.complete(Unit) }
        compose.waitUntil(10_000) { model.saveState.value == EditorSaveState.Saved }
    }

    @Test fun pinnedCardExposesItsStatusToScreenReader() {
        val note = Note("pinned", "Favorite", "Content", true, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithContentDescription("Pinned note").assertExists()
    }


    @Test fun clearSearchControlReallyRemovesQueryWithKeyboardFocused() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("draft query")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitUntil(5_000) { model.query.value.isEmpty() }
        compose.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        compose.onNodeWithTag("noma_new_note").assertExists()
    }

    @Test fun emptyArchiveOffersVisibleResetWithoutHidingCreate() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_archive_filter").performClick()
        compose.onNodeWithText("Your archive is empty").assertExists()
        compose.onNodeWithTag("noma_clear_filters").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { !model.archivedOnly.value }
        compose.onNodeWithTag("noma_new_note").assertExists()
    }


    @Test fun pendingExportMenuExplainsWhyBackupIsDisabled() {
        val repo = FakeRepository()
        compose.setContent {
            NomaTheme {
                PortableDocuments(
                    snapshot = NotesSnapshot.Ready(emptyList()),
                    onImport = { 0 },
                    snapshotForExport = { emptyList() },
                    onMessage = {},
                    canExport = false
                )
            }
        }
        compose.onNodeWithContentDescription("Import and export notes").performClick()
        compose.onNodeWithText("Finish saving or capturing before exporting").assertIsNotEnabled()
        compose.onNodeWithText("Check backup file").assertExists()
        compose.onNodeWithText("Import JSON").assertExists()
    }

    @Test fun confirmationDisablesExportIfNoteBecomesDirtyBeforeFinalConsent() {
        val ready = mutableStateOf(true)
        val repo = FakeRepository()
        compose.setContent {
            NomaTheme {
                PortableDocuments(
                    snapshot = NotesSnapshot.Ready(emptyList()),
                    onImport = { 0 },
                    snapshotForExport = { emptyList() },
                    onMessage = {},
                    canExport = ready.value
                )
            }
        }
        compose.onNodeWithContentDescription("Import and export notes").performClick()
        compose.onNodeWithText("Export JSON").performClick()
        compose.onNodeWithText("Unencrypted export").assertExists()
        compose.runOnIdle { ready.value = false }
        compose.onNodeWithText("Export JSON").assertIsNotEnabled()
        compose.onNodeWithText("This export cannot continue until note saves or captures finish, and any unsaved changes are resolved.").assertExists()
    }

    @Test fun existingNoteCardCannotInterruptSlowCreation() {
        val existing = Note("existing", "Existing note", "Stored", false, 1L, 1L)
        val repo = FakeRepository(listOf(existing))
        val gate = CompletableDeferred<Unit>()
        repo.saveGate = gate
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_new_note").performClick()
        compose.waitUntil(5_000) { model.creating.value }
        compose.onNodeWithTag("noma_note_existing").assertIsNotEnabled()
        compose.onNodeWithTag("noma_note_actions_existing").assertIsNotEnabled()
        compose.runOnIdle { gate.complete(Unit) }
        compose.waitUntilAtLeastOneExists(hasTestTag("noma_title_input"))
        compose.runOnIdle {
            assertEquals(2, (repo.notes.value as NotesSnapshot.Ready).entries.size)
        }
    }


    @Test fun exportReturnsToAvailableAfterSaveFinishes() {
        val ready = mutableStateOf(false)
        compose.setContent {
            NomaTheme {
                PortableDocuments(
                    snapshot = NotesSnapshot.Ready(emptyList()),
                    onImport = { 0 },
                    snapshotForExport = { emptyList() },
                    onMessage = {},
                    canExport = ready.value
                )
            }
        }
        compose.onNodeWithContentDescription("Import and export notes").performClick()
        compose.onNodeWithText("Finish saving or capturing before exporting").assertIsNotEnabled()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithText("Export JSON").assertExists()
    }


    @Test fun blankSharedBodyDisablesConfirmationWithAnExplanation() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Browser", ""))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("noma_share_validation_error").assertExists()
        compose.onNodeWithText("The shared text is empty. There is nothing to save.").assertExists()
        compose.runOnIdle { assertEquals(0, (repo.notes.value as NotesSnapshot.Ready).entries.size) }
    }

    @Test fun whitespaceOnlySharedBodyIsRejectedBeforeAnyWrite() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Browser", "   \n  "))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("noma_share_validation_error").assertExists()
        compose.runOnIdle { assertEquals(0, (repo.notes.value as NotesSnapshot.Ready).entries.size) }
    }

    @Test fun oversizedIncomingShareBodyExplainsCharacterLimit() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Website", "a".repeat(100_001)))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithText("Shared text exceeds the 100,000-character limit.").assertExists()
    }

    @Test fun oversizedIncomingShareTitleExplainsCharacterLimit() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("a".repeat(201), "Valid body"))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.onNodeWithText("Shared title exceeds the 200-character limit.").assertExists()
    }

    @Test fun storageLoadingMakesIncomingShareConfirmationUnavailable() {
        val repo = FakeRepository(initialState = NotesSnapshot.Loading)
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Website", "Valid body"))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("Valid body", model.pendingShare.value?.body) }
    }

    @Test fun storageErrorMakesIncomingShareConfirmationUnavailable() {
        val repo = FakeRepository(initialState = NotesSnapshot.Error("disk unavailable"))
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Website", "Valid body"))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("Valid body", model.pendingShare.value?.body) }
    }

    @Test fun pendingShareCanBeSavedAfterStorageBecomesReady() {
        val repo = FakeRepository(initialState = NotesSnapshot.Error("temporarily unavailable"))
        val model = NotesViewModel(repo, NotesService(repo))
        model.receiveSharedText(CaptureRequest.SharedText("Recovered", "Captured text"))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_share_confirm").assertIsNotEnabled()
        compose.runOnIdle { repo.updateSnapshot(NotesSnapshot.Ready(emptyList())) }
        compose.onNodeWithTag("noma_share_confirm").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { model.pendingShare.value == null }
        compose.runOnIdle {
            assertEquals("Captured text", (repo.notes.value as NotesSnapshot.Ready).entries.single().body)
            assertEquals("Captured text", model.active.value?.body)
        }
    }


    @Test fun searchPillIsTheOnlySearchFieldAndDockedBelowNotes() {
        val saved = Note("a", "Weekend notes", "Body", false, 1L, 1L)
        val repo = FakeRepository(listOf(saved))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onAllNodesWithTag("noma_search").assertCountEquals(1)
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val pill = compose.onNodeWithTag("noma_search_pill").fetchSemanticsNode().boundsInRoot
        assertTrue("Search must be in the lower half", pill.center.y > root.center.y)
        assertTrue("Search must fit screen width", pill.left >= root.left && pill.right <= root.right)
        compose.onNodeWithText("Weekend notes").assertExists()
    }

    @Test fun bottomPillFiltersExistingNoteWithoutChangingTheEditor() {
        val saved = Note("a", "Weekend notes", "Draft shopping list", false, 1L, 1L)
        val repo = FakeRepository(listOf(saved))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("shopping")
        compose.waitUntil(5_000) { model.query.value == "shopping" }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.onNodeWithText("Weekend notes").assertExists()
        compose.runOnIdle {
            assertEquals(saved, (repo.notes.value as NotesSnapshot.Ready).entries.single())
            assertEquals(null, model.active.value)
        }
    }

    @Test fun bottomPillCanClearAQueryWithoutRemovingTheSearchBar() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("no matches")
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitUntil(5_000) { model.query.value.isEmpty() }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.onAllNodesWithTag("noma_search").assertCountEquals(1)
    }

    @Test fun bottomSearchDisablesInputWhenLocalStorageIsUnavailable() {
        val repo = FakeRepository(initialState = NotesSnapshot.Error("unavailable"))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.onNodeWithTag("noma_search").assertIsNotEnabled()
        compose.onNodeWithTag("noma_new_note").assertDoesNotExist()
    }


    @Test fun clearFiltersRemainsDirectlyAboveBottomSearchWhenKeyboardOpen() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("missing note")
        compose.waitUntil(5_000) { model.query.value == "missing note" }
        val reset = compose.onNodeWithTag("noma_clear_filters")
        reset.assertIsDisplayed()
        val resetBounds = reset.fetchSemanticsNode().boundsInRoot
        val pillBounds = compose.onNodeWithTag("noma_search_pill")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Reset action must be above the bottom pill", resetBounds.bottom <= pillBounds.top)
        reset.performClick()
        compose.waitUntil(5_000) { model.query.value.isEmpty() }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
    }

    @Test fun failedSaveRetryIsPinnedAboveScrollWhileKeyboardIsOpen() {
        val note = Note("save-bar", "Draft title", "Persisted", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        repo.failWrites = true
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithText("Draft title").performClick()
        compose.onNodeWithTag("noma_body_input").performTextInput(" changed")
        compose.waitUntil(10_000) { model.saveState.value is EditorSaveState.Failed }
        compose.onNodeWithTag("noma_editor_save_bar").assertIsDisplayed()
        compose.onNodeWithTag("noma_retry_save").assertIsDisplayed()
        compose.runOnIdle { assertEquals("save-bar", model.active.value?.id) }
    }

    @Test fun newAppearanceChoicesRemainAccessibleAfterTheVisualRedesign() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        val chosen = mutableStateOf(NomaAppearance.SYSTEM)
        compose.setContent {
            NomaTheme {
                NomaApp(
                    viewModel = model,
                    appearance = chosen.value,
                    onAppearanceChange = { chosen.value = it }
                )
            }
        }
        compose.onNodeWithContentDescription("Appearance").performClick()
        compose.onNodeWithText("Follow system").assertExists()
        compose.onNodeWithText("Light").assertExists()
        compose.onNodeWithText("Dark").performClick()
        compose.runOnIdle { assertEquals(NomaAppearance.DARK, chosen.value) }
    }


    @Test fun searchFocusShowsSeparateCancelAndPreservesTypedQuery() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("draft")
        compose.onNodeWithTag("noma_search_dismiss").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { model.query.value == "draft" }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.runOnIdle { assertEquals("draft", model.query.value) }
    }

    @Test fun clearingBottomSearchKeepsFocusForAnotherQuery() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("first")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitUntil(5_000) { model.query.value.isEmpty() }
        compose.onNodeWithTag("noma_search_dismiss").assertExists()
        compose.onNodeWithTag("noma_search").performTextInput("second")
        compose.waitUntil(5_000) { model.query.value == "second" }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
    }

    @Test fun activeSearchShowsOneResultsSummaryNearBottomPill() {
        val note = Note("one", "Travel plan", "Train tickets", false, 1L, 1L)
        val repo = FakeRepository(listOf(note))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("Travel")
        compose.waitUntil(5_000) { model.query.value == "Travel" }
        compose.onNodeWithTag("noma_search_result_count").assertIsDisplayed()
        compose.onAllNodesWithText("1 result").assertCountEquals(1)
        val result = compose.onNodeWithTag("noma_search_result_count")
            .fetchSemanticsNode().boundsInRoot
        val search = compose.onNodeWithTag("noma_search_pill")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Result summary should be adjacent to bottom search", result.bottom <= search.top)
    }

    @Test fun emptySearchHasNoResultsSummaryUntilQueryIsEntered() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search_result_count").assertDoesNotExist()
        compose.onNodeWithTag("noma_search").performTextInput("nothing")
        compose.waitUntil(5_000) { model.query.value == "nothing" }
        compose.onNodeWithText("0 results").assertExists()
        compose.onNodeWithTag("noma_search_result_count").assertIsDisplayed()
    }

    @Test fun searchHintReflectsArchivedScopeWithoutChangingTheQuery() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_archive_filter").performClick()
        compose.onNodeWithText("Search archived notes").assertExists()
        compose.onNodeWithTag("noma_archive_filter").assertIsSelected()
        compose.onNodeWithTag("noma_search").performTextInput("memo")
        compose.waitUntil(5_000) { model.query.value == "memo" && model.archivedOnly.value }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
    }

    @Test fun pinnedScopeIsLabeledAndItsFilterRemainsDirectlyTappable() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_pinned_filter").performClick()
        compose.onNodeWithTag("noma_pinned_filter").assertIsSelected()
        compose.onNodeWithText("Search pinned notes").assertExists()
        compose.onNodeWithTag("noma_pinned_filter").performClick()
        compose.waitUntil(5_000) { !model.pinnedOnly.value }
        compose.onNodeWithText("Search your notes").assertExists()
    }

    @Test fun bottomQuerySurvivesChangingSortOrder() {
        val first = Note("one", "Zebra", "Memo", false, 1L, 1L)
        val second = Note("two", "Apple", "Memo", false, 1L, 1L)
        val repo = FakeRepository(listOf(first, second))
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("Memo")
        compose.onNodeWithTag("noma_sort_button").performClick()
        compose.onNodeWithText("Title (A–Z)").performClick()
        compose.runOnIdle {
            assertEquals("Memo", model.query.value)
            assertEquals(2, model.visible(
                (repo.notes.value as NotesSnapshot.Ready).entries,
                model.query.value, false, false, model.sort.value
            ).size)
        }
        compose.onNodeWithTag("noma_search_result_count").assertIsDisplayed()
    }

    @Test fun newNoteButtonRemainsAccessibleWhileSearchIsFocused() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_search").performTextInput("draft")
        compose.onNodeWithTag("noma_new_note").assertExists()
        compose.onNodeWithContentDescription("New note").assertExists()
        compose.onNodeWithTag("noma_search_dismiss").assertIsDisplayed()
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
    }

    @Test fun filtersAndQueryCanBeResetTogetherWithoutRemovingSearch() {
        val repo = FakeRepository()
        val model = NotesViewModel(repo, NotesService(repo))
        compose.setContent { NomaTheme { NomaApp(model) } }
        compose.onNodeWithTag("noma_pinned_filter").performClick()
        compose.onNodeWithTag("noma_search").performTextInput("inexistent")
        compose.onNodeWithTag("noma_clear_filters").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            model.query.value.isEmpty() && !model.pinnedOnly.value && !model.archivedOnly.value
        }
        compose.onNodeWithTag("noma_search_pill").assertIsDisplayed()
        compose.onNodeWithTag("noma_pinned_filter").assertExists()
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
