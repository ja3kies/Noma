package dev.juuri.noma.ui

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import dev.juuri.noma.R
import dev.juuri.noma.capture.NomaWidgetStyle
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.domain.NoteSearch
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.ui.theme.NomaAppearance
import dev.juuri.noma.ui.theme.NomaMotion
import dev.juuri.noma.ui.theme.NomaSpacing
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * A platform-native Noma shell: narrow windows navigate between list and editor;
 * wide windows keep a 400dp list beside a comfortably constrained editor.
 */
@Composable
fun NomaApp(
    viewModel: NotesViewModel,
    appearance: NomaAppearance = NomaAppearance.SYSTEM,
    onAppearanceChange: (NomaAppearance) -> Unit = {},
    widgetStyle: NomaWidgetStyle = NomaWidgetStyle.MATERIAL_YOU,
    onWidgetStyleChange: (NomaWidgetStyle) -> Unit = {}
) {
    val active by viewModel.active.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val pendingShare by viewModel.pendingShare.collectAsState()
    val capturingShare by viewModel.capturingShare.collectAsState()
    val creatingNote by viewModel.creating.collectAsState()
    val storageState by viewModel.snapshot.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(R.string.deleted_message)
    val archived = stringResource(R.string.archived_message)
    val restored = stringResource(R.string.unarchived_message)
    val undo = stringResource(R.string.undo)
    val failurePrefix = stringResource(R.string.error_prefix)
    val importResult = stringResource(R.string.portable_import_result)
    val sharedCaptureSaved = stringResource(R.string.incoming_share_saved)
    val createdWhileEditing = stringResource(R.string.created_while_editing)
    val shareSavedWhileEditing = stringResource(R.string.shared_saved_while_editing)
    val shareNeedsSave = stringResource(R.string.incoming_share_save_current_first)
    val saveBeforeNav = stringResource(R.string.save_before_navigation)
    val deferredCaptureLabel = stringResource(R.string.quick_capture_deferred)
    val busyCaptureLabel = stringResource(R.string.quick_capture_busy)
    val unavailableStorageLabel = stringResource(R.string.notes_storage_not_ready)
    val sharedEmptyLabel = stringResource(R.string.share_empty_body)
    val sharedLongLabel = stringResource(R.string.share_body_too_long)
    val sharedTitleLabel = stringResource(R.string.share_title_too_long)
    val shareQueueFullLabel = stringResource(R.string.share_queue_full)
    val captureInProgressLabel = stringResource(R.string.capture_in_progress)

    LaunchedEffect(viewModel, deleted, archived, restored, undo, failurePrefix, importResult, saveBeforeNav, sharedCaptureSaved, createdWhileEditing, shareSavedWhileEditing, deferredCaptureLabel, busyCaptureLabel, unavailableStorageLabel, sharedEmptyLabel, sharedLongLabel, sharedTitleLabel, shareQueueFullLabel, captureInProgressLabel) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteUiEvent.Deleted -> {
                    val result = snackbar.showSnackbar(deleted, actionLabel = undo, withDismissAction = true)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.note)
                }
                is NoteUiEvent.ArchiveChanged -> {
                    val message = if (event.previous.isArchived) restored else archived
                    val result = snackbar.showSnackbar(message, actionLabel = undo, withDismissAction = true)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoArchive(event.previous)
                }
                is NoteUiEvent.Imported -> snackbar.showSnackbar(importResult.format(event.added, event.skipped))
                NoteUiEvent.SaveBeforeNavigation -> snackbar.showSnackbar(saveBeforeNav)
                NoteUiEvent.QuickCaptureDeferred -> snackbar.showSnackbar(deferredCaptureLabel)
                NoteUiEvent.QuickCaptureBusy -> snackbar.showSnackbar(busyCaptureLabel)
                NoteUiEvent.StorageNotReady -> snackbar.showSnackbar(unavailableStorageLabel)
                NoteUiEvent.SharedTextEmpty -> snackbar.showSnackbar(sharedEmptyLabel)
                NoteUiEvent.SharedTextTooLong -> snackbar.showSnackbar(sharedLongLabel)
                NoteUiEvent.SharedTitleTooLong -> snackbar.showSnackbar(sharedTitleLabel)
                NoteUiEvent.ShareQueueFull -> snackbar.showSnackbar(shareQueueFullLabel)
                NoteUiEvent.CaptureInProgress -> snackbar.showSnackbar(captureInProgressLabel)
                NoteUiEvent.SharedCaptureSaved -> snackbar.showSnackbar(sharedCaptureSaved)
                NoteUiEvent.CreatedWhileEditing -> snackbar.showSnackbar(createdWhileEditing)
                NoteUiEvent.SharedCaptureSavedWhileEditing -> snackbar.showSnackbar(shareSavedWhileEditing)
                is NoteUiEvent.Failure ->
                    snackbar.showSnackbar(failurePrefix + event.message, withDismissAction = true)
            }
        }
    }

    pendingShare?.let { incoming ->
        val invalidShareReason = when {
            incoming.body.isBlank() -> stringResource(R.string.share_empty_body)
            incoming.body.length > 100_000 -> stringResource(R.string.share_body_too_long)
            incoming.title.length > 200 -> stringResource(R.string.share_title_too_long)
            else -> null
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissSharedText,
            title = { Text(stringResource(R.string.incoming_share_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                    Text(stringResource(R.string.incoming_share_body))
                    if (incoming.title.isNotEmpty()) {
                        Text(
                            incoming.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        incoming.body.take(500),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (incoming.body.length > 500) {
                        Text(
                            stringResource(
                                R.string.incoming_share_preview_truncated,
                                500,
                                incoming.body.length
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.incoming_share_size, incoming.body.length),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (invalidShareReason != null) {
                        Text(
                            invalidShareReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("noma_share_validation_error")
                        )
                    }
                    if (active != null && saveState != EditorSaveState.Saved) {
                        Text(
                            shareNeedsSave,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("noma_share_save_guidance")
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !capturingShare && !creatingNote &&
                        storageState is NotesSnapshot.Ready && invalidShareReason == null &&
                        (active == null || saveState == EditorSaveState.Saved),
                    onClick = viewModel::confirmSharedText,
                    modifier = Modifier.testTag("noma_share_confirm")
                ) { Text(stringResource(R.string.incoming_share_confirm)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !capturingShare,
                    onClick = viewModel::dismissSharedText
                ) { Text(stringResource(R.string.incoming_share_discard)) }
            }
        )
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 840.dp
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    NotesListScreen(
                        viewModel = viewModel,
                        snackbar = snackbar,
                        selectedId = active?.id,
                        appearance = appearance,
                        onAppearanceChange = onAppearanceChange,
                        widgetStyle = widgetStyle,
                        onWidgetStyleChange = onWidgetStyleChange,
                        modifier = Modifier.width(NomaSpacing.listPaneWidth).fillMaxHeight()
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (active == null) {
                            EmptyEditorPane()
                        } else {
                            NoteEditorScreen(
                                note = requireNotNull(active),
                                saveState = saveState,
                                viewModel = viewModel,
                                snackbar = snackbar,
                                expanded = true
                            )
                        }
                    }
                }
            } else {
                if (active == null) {
                    NotesListScreen(
                        viewModel = viewModel,
                        snackbar = snackbar,
                        selectedId = null,
                        appearance = appearance,
                        onAppearanceChange = onAppearanceChange,
                        widgetStyle = widgetStyle,
                        onWidgetStyleChange = onWidgetStyleChange,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    NoteEditorScreen(
                        note = requireNotNull(active),
                        saveState = saveState,
                        viewModel = viewModel,
                        snackbar = snackbar,
                        expanded = false
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesListScreen(
    viewModel: NotesViewModel,
    snackbar: SnackbarHostState,
    selectedId: String?,
    appearance: NomaAppearance,
    onAppearanceChange: (NomaAppearance) -> Unit,
    widgetStyle: NomaWidgetStyle,
    onWidgetStyleChange: (NomaWidgetStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val query by viewModel.query.collectAsState()
    val pinnedOnly by viewModel.pinnedOnly.collectAsState()
    val archivedOnly by viewModel.archivedOnly.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val creating by viewModel.creating.collectAsState()
    val editorSaveState by viewModel.saveState.collectAsState()
    val capturingShare by viewModel.capturingShare.collectAsState()
    val navigationEnabled = !creating && !capturingShare
    val filteredNotes = remember(snapshot, query, pinnedOnly, archivedOnly, sort) {
        (snapshot as? NotesSnapshot.Ready)?.let { state ->
            viewModel.visible(state.entries, query, pinnedOnly, archivedOnly, sort)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(query, pinnedOnly, archivedOnly, sort) {
        if (!filteredNotes.isNullOrEmpty()) listState.scrollToItem(0)
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val searchKeyboard = LocalSoftwareKeyboardController.current
    val portableScope = rememberCoroutineScope()
    val searchAccessibilityLabel = stringResource(R.string.search_hint)

    var deleting by remember { mutableStateOf<Note?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showWidgetStyles by remember { mutableStateOf(false) }

    deleting?.let { note ->
        DeleteConfirmation(
            noteTitle = note.displayTitle.ifBlank { stringResource(R.string.untitled) },
            onCancel = { deleting = null },
            onConfirm = {
                viewModel.deleteNote(note)
                deleting = null
            }
        )
    }
    if (showAppearance) {
        AppearanceDialog(
            selected = appearance,
            onSelect = {
                onAppearanceChange(it)
                showAppearance = false
            },
            onDismiss = { showAppearance = false },
            onWidgetStyles = {
                showAppearance = false
                showWidgetStyles = true
            }
        )
    }
    if (showWidgetStyles) {
        WidgetStyleDialog(
            selected = widgetStyle,
            onSelect = {
                onWidgetStyleChange(it)
                showWidgetStyles = false
            },
            onDismiss = { showWidgetStyles = false }
        )
    }

    val newNoteLabel = stringResource(R.string.new_note)
    val creatingNoteLabel = stringResource(R.string.creating_note)
    val creatingOrCapturing = creating || capturingShare
    val currentSortLabel = stringResource(if (sort == NoteSort.RECENT) R.string.sort_recent else R.string.sort_title)
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        // Like a bottom-toolbar search field on iOS, but implemented with
        // Compose insets and native Material colors. The Scaffold reserves
        // room for this bar and positions its FAB above it automatically.
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .animateContentSize(
                        spring(dampingRatio = Spring.DampingRatioNoBouncy)
                    )
                    .padding(horizontal = NomaSpacing.comfortable, vertical = NomaSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Keep recovery reachable even while the keyboard obscures the list.
                if (snapshot is NotesSnapshot.Ready &&
                    filteredNotes.isNullOrEmpty() &&
                    (query.isNotBlank() || pinnedOnly || archivedOnly)
                ) {
                    TextButton(
                        modifier = Modifier.testTag("noma_clear_filters"),
                        onClick = {
                            focusManager.clearFocus()
                            searchKeyboard?.hide()
                            viewModel.clearDiscoveryFilters()
                        }
                    ) {
                        Text(stringResource(R.string.clear_all_filters))
                    }
                }
                BottomSearchPill(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    enabled = snapshot is NotesSnapshot.Ready,
                    accessibilityLabel = searchAccessibilityLabel,
                    onSearchDone = {
                        focusManager.clearFocus()
                        searchKeyboard?.hide()
                    },
                    onClear = {
                        focusManager.clearFocus()
                        searchKeyboard?.hide()
                        viewModel.onQueryChange("")
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
        },
        floatingActionButton = {
            // Offer the same creation action in empty, filtered and populated libraries.
            // Keep it hidden while storage is loading or unavailable.
            if (snapshot is NotesSnapshot.Ready) {
                ExtendedFloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        searchKeyboard?.hide()
                        if (!creatingOrCapturing) viewModel.createNote()
                    },
                    expanded = true,
                    icon = {
                        if (creatingOrCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).testTag("noma_creation_spinner"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        }
                    },
                    text = {
                        Text(if (creatingOrCapturing) stringResource(R.string.creating_note) else newNoteLabel)
                    },
                    containerColor = if (creatingOrCapturing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (creatingOrCapturing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("noma_new_note").semantics {
                        if (creatingOrCapturing) {
                            disabled()
                            contentDescription = creatingNoteLabel
                        }
                    }
                )
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NomaSpacing.page, end = NomaSpacing.page, top = 20.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showAppearance = true }) {
                        Icon(Icons.Outlined.Palette, contentDescription = stringResource(R.string.appearance))
                    }
                    PortableDocuments(
                        snapshot = snapshot,
                        onImport = viewModel::importNotesAwait,
                        snapshotForExport = viewModel::snapshotForExport,
                        canExport = editorSaveState == EditorSaveState.Saved && navigationEnabled,
                        onMessage = { message ->
                            portableScope.launch { snackbar.showSnackbar(message) }
                        }
                    )
                }
                Spacer(Modifier.height(NomaSpacing.medium))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)
                    ) {
                        FilterChip(
                            selected = archivedOnly,
                            onClick = {
                                focusManager.clearFocus()
                                searchKeyboard?.hide()
                                viewModel.toggleArchiveView()
                            },
                            enabled = snapshot is NotesSnapshot.Ready,
                            modifier = Modifier.testTag("noma_archive_filter"),
                            label = { Text(stringResource(R.string.archive_view)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Archive, contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        if (!archivedOnly) {
                            FilterChip(
                                selected = pinnedOnly,
                                onClick = {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    viewModel.togglePinnedOnly()
                                },
                                enabled = snapshot is NotesSnapshot.Ready,
                                label = { Text(stringResource(R.string.pinned_filter)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.PushPin, contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            enabled = snapshot is NotesSnapshot.Ready,
                            modifier = Modifier.testTag("noma_sort_button").semantics {
                                stateDescription = currentSortLabel
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.sort_notes))
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_recent)) },
                                modifier = Modifier.semantics { selected = sort == NoteSort.RECENT },
                                onClick = {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    viewModel.selectSort(NoteSort.RECENT)
                                    sortMenuExpanded = false
                                },
                                trailingIcon = if (sort == NoteSort.RECENT) {
                                    { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title)) },
                                modifier = Modifier.semantics { selected = sort == NoteSort.TITLE },
                                onClick = {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    viewModel.selectSort(NoteSort.TITLE)
                                    sortMenuExpanded = false
                                },
                                trailingIcon = if (sort == NoteSort.TITLE) {
                                    { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
                if (snapshot is NotesSnapshot.Ready) {
                    val count = (snapshot as NotesSnapshot.Ready).entries.count {
                        it.isArchived == archivedOnly
                    }
                    val filteredCount = filteredNotes?.size ?: 0
                    Text(
                        text = if (query.isNotBlank() || pinnedOnly)
                            pluralStringResource(R.plurals.search_results_total, filteredCount, filteredCount)
                        else pluralStringResource(R.plurals.note_total, count, count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }

            when (val state = snapshot) {
                NotesSnapshot.Loading -> CenterMessage {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(NomaSpacing.comfortable))
                    Text(stringResource(R.string.loading_notes))
                }
                is NotesSnapshot.Error -> CenterMessage {
                    Icon(
                        Icons.Outlined.ErrorOutline, contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(NomaSpacing.medium))
                    Text(stringResource(R.string.unavailable), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(NomaSpacing.small))
                    Text(
                        stringResource(R.string.error_recovery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(NomaSpacing.comfortable))
                    OutlinedButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }
                is NotesSnapshot.Ready -> {
                    val visible = filteredNotes.orEmpty()
                    if (visible.isEmpty()) {
                        CenterMessage {
                            Icon(
                                when {
                                    query.isNotBlank() || pinnedOnly -> Icons.Outlined.Search
                                    archivedOnly -> Icons.Outlined.Archive
                                    else -> Icons.Outlined.EditNote
                                },
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(NomaSpacing.comfortable))
                            Text(
                                stringResource(
                                    when {
                                        query.isNotBlank() || pinnedOnly -> R.string.no_results
                                        archivedOnly -> R.string.empty_archive
                                        else -> R.string.empty_title
                                    }
                                ),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(NomaSpacing.small))
                            Text(
                                stringResource(
                                    when {
                                        query.isNotBlank() || pinnedOnly -> R.string.no_results_detail
                                        archivedOnly -> R.string.empty_archive_detail
                                        else -> R.string.empty_detail
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val pinned = if (archivedOnly) emptyList() else visible.filter { it.isPinned }
                        val others = if (archivedOnly) visible else visible.filterNot { it.isPinned }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("noma_note_list"),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = NomaSpacing.comfortable,
                                end = NomaSpacing.comfortable,
                                bottom = 108.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pinned.isNotEmpty()) {
                                item(key = "pinned_section") {
                                    SectionHeader(stringResource(R.string.pinned_section))
                                }
                                items(pinned, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        query = query,
                                        selected = note.id == selectedId,
                                        onOpen = {
                                            focusManager.clearFocus()
                                            searchKeyboard?.hide()
                                            viewModel.openNote(note)
                                        },
                                        onPin = { viewModel.togglePin(note) },
                                        onArchive = { viewModel.toggleArchive(note) },
                                        onDelete = { deleting = note },
                                        onShare = { shareNote(context, note) },
                                        actionsEnabled = note.id != selectedId || editorSaveState == EditorSaveState.Saved,
                                        navigationEnabled = navigationEnabled,
                                        onMenuOpen = {
                                            focusManager.clearFocus()
                                            searchKeyboard?.hide()
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                            if (others.isNotEmpty()) {
                                item(key = "recent_section") {
                                    SectionHeader(
                                        stringResource(
                                            when {
                                                archivedOnly -> R.string.archived_section
                                                query.isNotBlank() -> R.string.search_section
                                                sort == NoteSort.TITLE -> R.string.alphabetical_section
                                                else -> R.string.recent_section
                                            }
                                        )
                                    )
                                }
                                items(others, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        query = query,
                                        selected = note.id == selectedId,
                                        onOpen = {
                                            focusManager.clearFocus()
                                            searchKeyboard?.hide()
                                            viewModel.openNote(note)
                                        },
                                        onPin = { viewModel.togglePin(note) },
                                        onArchive = { viewModel.toggleArchive(note) },
                                        onDelete = { deleting = note },
                                        onShare = { shareNote(context, note) },
                                        actionsEnabled = note.id != selectedId || editorSaveState == EditorSaveState.Saved,
                                        navigationEnabled = navigationEnabled,
                                        onMenuOpen = {
                                            focusManager.clearFocus()
                                            searchKeyboard?.hide()
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Persistent, thumb-reachable search field. The IME lifts the bottom toolbar
 * through WindowInsets rather than a manually calculated keyboard height.
 * Color, shape and elevation are semantic Material 3 values on every theme.
 */
@Composable
private fun BottomSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    enabled: Boolean,
    accessibilityLabel: String,
    onSearchDone: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (focused) 8.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nomaBottomSearchElevation"
    )
    Surface(
        modifier = modifier.fillMaxWidth().testTag("noma_search_pill"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = accessibilityLabel }
                .testTag("noma_search"),
            singleLine = true,
            shape = CircleShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = onClear, enabled = enabled) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = NomaSpacing.small, top = NomaSpacing.medium, bottom = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun highlightMatches(value: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return remember(value, query, accent) {
        buildAnnotatedString {
            append(value)
            NoteSearch.matchRanges(value, query).forEach { range ->
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = accent),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    query: String,
    selected: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    actionsEnabled: Boolean,
    navigationEnabled: Boolean,
    onMenuOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember(note.id) { mutableStateOf(false) }
    LaunchedEffect(navigationEnabled) {
        if (!navigationEnabled) menuExpanded = false
    }
    val title = note.displayTitle.ifBlank { stringResource(R.string.untitled) }
    val relevantPreview = remember(note.title, note.body, query) {
        NoteSearch.matchingPreview(note, query)
    }
    val cardColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow
    Card(
        onClick = onOpen,
        enabled = navigationEnabled,
        modifier = modifier.fillMaxWidth()
            .testTag("noma_note_${note.id}")
            .semantics { this.selected = selected }
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        border = if (selected) BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        ) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = 88.dp)
                .padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = highlightMatches(title, query),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (relevantPreview.isNotEmpty() &&
                    (note.title.isNotBlank() || relevantPreview != note.displayTitle)) {
                    Spacer(Modifier.height(NomaSpacing.tiny))
                    Text(
                        text = highlightMatches(relevantPreview, query),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(NomaSpacing.small))
                Text(
                    text = formatEdited(note.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (note.isPinned && !note.isArchived) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = stringResource(R.string.pinned_note_indicator),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp).size(18.dp)
                )
            }
            Box {
                IconButton(
                    onClick = {
                        onMenuOpen()
                        menuExpanded = true
                    },
                    enabled = navigationEnabled,
                    modifier = Modifier.testTag("noma_note_actions_${note.id}")
                ) {
                    Icon(
                        Icons.Outlined.MoreHoriz,
                        contentDescription = stringResource(R.string.note_actions_for, title)
                    )
                }
                NoteActionsMenu(
                    expanded = menuExpanded,
                    note = note,
                    onDismiss = { menuExpanded = false },
                    onPin = onPin,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onShare = onShare,
                    destructiveEnabled = actionsEnabled && navigationEnabled
                )
            }
        }
    }
}

@Composable
private fun NoteActionsMenu(
    expanded: Boolean,
    note: Note,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    showPinAction: Boolean = true,
    destructiveEnabled: Boolean = true
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // An archived note is not shown in the pinned section. Do not offer
        // a pin action that appears to have no visible effect.
        if (showPinAction && !note.isArchived) {
            DropdownMenuItem(
                text = { Text(stringResource(if (note.isPinned) R.string.unpin_note else R.string.pin_note)) },
                enabled = destructiveEnabled,
                onClick = { onDismiss(); onPin() },
                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(if (note.isArchived) R.string.unarchive_note else R.string.archive_note)) },
            enabled = destructiveEnabled,
            onClick = { onDismiss(); onArchive() },
            leadingIcon = {
                Icon(
                    if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.share_note)) },
            enabled = destructiveEnabled && (note.title.isNotBlank() || note.body.isNotBlank()),
            onClick = { onDismiss(); onShare() },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_note)) },
            enabled = destructiveEnabled,
            onClick = { onDismiss(); onDelete() },
            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note: Note,
    saveState: EditorSaveState,
    viewModel: NotesViewModel,
    snackbar: SnackbarHostState,
    expanded: Boolean
) {
    var confirmDelete by rememberSaveable(note.id) { mutableStateOf(false) }
    var menuExpanded by remember(note.id) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(note.id) { mutableStateOf(false) }
    var closeWhenSaved by rememberSaveable(note.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val titleFocus = remember(note.id) { FocusRequester() }
    val bodyFocus = remember(note.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun requestClose() {
        menuExpanded = false
        when (saveState) {
            EditorSaveState.Saved -> viewModel.closeNote()
            EditorSaveState.Saving -> closeWhenSaved = true
            is EditorSaveState.Failed -> confirmDiscard = true
        }
    }

    // Let Android consume Back to dismiss the IME first. A second Back
    // navigates away only after the keyboard is no longer visible.
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    BackHandler(enabled = !keyboardVisible) { requestClose() }
    // Back while writing waits for the last disk acknowledgement.
    // A failed write keeps the unsaved content in the editor for retry.
    LaunchedEffect(closeWhenSaved, saveState) {
        if (closeWhenSaved) {
            when (saveState) {
                EditorSaveState.Saved -> {
                    closeWhenSaved = false
                    viewModel.closeNote()
                }
                EditorSaveState.Saving -> Unit
                is EditorSaveState.Failed -> {
                    closeWhenSaved = false
                    confirmDiscard = true
                }
            }
        }
    }
    LaunchedEffect(note.id) {
        if (note.title.isBlank() && note.body.isBlank()) {
            titleFocus.requestFocus()
            keyboard?.show()
        }
    }
    if (confirmDelete) {
        DeleteConfirmation(
            noteTitle = note.displayTitle.ifBlank { stringResource(R.string.untitled) },
            onCancel = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.deleteNote(note)
            }
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.unsaved_title)) },
            text = { Text(stringResource(R.string.unsaved_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.retrySave()
                }, modifier = Modifier.testTag("noma_dialog_retry_save")) {
                    Text(stringResource(R.string.retry))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            confirmDiscard = false
                            closeWhenSaved = false
                            viewModel.cancelDeferredQuickNote()
                        },
                        modifier = Modifier.testTag("noma_keep_editing")
                    ) { Text(stringResource(R.string.keep_editing)) }
                    TextButton(
                        onClick = {
                            confirmDiscard = false
                            viewModel.closeNote()
                        },
                        modifier = Modifier.testTag("noma_discard_unsaved")
                    ) {
                        Text(
                            stringResource(R.string.leave_unsaved),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }
    val safeTitle = stringResource(R.string.editor_nav_title)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = if (!expanded) ({ SnackbarHost(snackbar) }) else ({ }),
        topBar = {
            Column(Modifier.animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))) {
                TopAppBar(
                title = {
                    Text(
                        if (expanded) stringResource(R.string.editor_title) else safeTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::requestClose,
                        modifier = Modifier.testTag("noma_editor_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_notes)
                        )
                    }
                },
                actions = {
                    val portableScope = rememberCoroutineScope()
                    PortableNoteExport(note, enabled = saveState == EditorSaveState.Saved, onMessage = { message ->
                        portableScope.launch { snackbar.showSnackbar(message) }
                    })
                    IconButton(
                        onClick = { viewModel.togglePin(note) },
                        enabled = saveState == EditorSaveState.Saved && !note.isArchived,
                        modifier = Modifier.testTag("noma_editor_pin")
                    ) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (note.isPinned) R.string.unpin_note else R.string.pin_note
                            ),
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("noma_editor_actions")
                        ) {
                            Icon(
                                Icons.Outlined.MoreHoriz,
                                contentDescription = stringResource(R.string.note_actions)
                            )
                        }
                        NoteActionsMenu(
                            expanded = menuExpanded,
                            note = note,
                            onDismiss = { menuExpanded = false },
                            onPin = { viewModel.togglePin(note) },
                            onArchive = { viewModel.toggleArchive(note) },
                            onDelete = { confirmDelete = true },
                            onShare = { shareNote(context, note) },
                            showPinAction = false,
                            destructiveEnabled = saveState == EditorSaveState.Saved
                        )
                    }
                }
                )
                // A save failure must stay actionable even while the editor has
                // scrolled and the IME is visible; never bury Retry in the document.
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = NomaSpacing.page, vertical = NomaSpacing.tiny)
                            .testTag("noma_editor_save_bar")
                    ) {
                        EditorSaveStatus(saveState = saveState, onRetry = viewModel::retrySave)
                    }
                }
            }
        }
    ) { insets ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(insets).imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val width = if (maxWidth > NomaSpacing.editorMaxWidth)
                NomaSpacing.editorMaxWidth else maxWidth
            Column(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = NomaSpacing.page, end = NomaSpacing.page, top = 16.dp, bottom = 64.dp)
            ) {
                if (saveState is EditorSaveState.Failed) {
                    Text(
                        stringResource(R.string.save_failure_guidance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("noma_save_failure_guidance")
                    )
                }
                if (closeWhenSaved && saveState == EditorSaveState.Saving) {
                    Text(
                        stringResource(R.string.wait_for_save_before_exit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("noma_pending_exit")
                    )
                }
                Spacer(Modifier.height(24.dp))
                BasicTextField(
                    value = note.title,
                    onValueChange = {
                        closeWhenSaved = false
                        viewModel.changeTitle(it)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(titleFocus).testTag("noma_title_input"),
                    decorationBox = { input ->
                        Box {
                            if (note.title.isEmpty()) {
                                Text(
                                    stringResource(R.string.title_hint),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            input()
                        }
                    }
                )
                // The document is one continuous writing surface, not two forms.
                Spacer(Modifier.height(24.dp))
                BasicTextField(
                    value = note.body,
                    onValueChange = {
                        closeWhenSaved = false
                        viewModel.changeBody(it)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 26.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
                        .focusRequester(bodyFocus).testTag("noma_body_input"),
                    decorationBox = { input ->
                        Box {
                            if (note.body.isEmpty()) {
                                Text(
                                    stringResource(R.string.body_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            input()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorSaveStatus(saveState: EditorSaveState, onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .animateContentSize(tween(NomaMotion.quick))
    ) {
        when (saveState) {
            EditorSaveState.Saved -> {
                Icon(
                    Icons.Outlined.CheckCircleOutline, contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(NomaSpacing.small))
                Text(
                    stringResource(R.string.saved_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EditorSaveState.Saving -> {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(NomaSpacing.small))
                Text(
                    stringResource(R.string.saving_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is EditorSaveState.Failed -> {
                Icon(
                    Icons.Outlined.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(NomaSpacing.small))
                Text(
                    stringResource(R.string.save_failed_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRetry, modifier = Modifier.testTag("noma_retry_save")) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorPane() {
    CenterMessage {
        Icon(
            Icons.Outlined.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(NomaSpacing.comfortable))
        Text(
            stringResource(R.string.select_note_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(NomaSpacing.small))
        Text(
            stringResource(R.string.select_note_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppearanceDialog(
    selected: NomaAppearance,
    onSelect: (NomaAppearance) -> Unit,
    onDismiss: () -> Unit,
    onWidgetStyles: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance)) },
        text = {
            Column(Modifier.selectableGroup().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                val options = listOf(
                    NomaAppearance.SYSTEM to R.string.appearance_system,
                    NomaAppearance.LIGHT to R.string.appearance_light,
                    NomaAppearance.DARK to R.string.appearance_dark
                )
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .selectable(
                                selected = mode == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Spacer(Modifier.width(NomaSpacing.comfortable))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            TextButton(onClick = onWidgetStyles) {
                Text(stringResource(R.string.widget_style_settings))
            }
        }
    )
}

@Composable
private fun WidgetStyleDialog(
    selected: NomaWidgetStyle,
    onSelect: (NomaWidgetStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val styles = listOf(
        NomaWidgetStyle.MATERIAL_YOU to R.string.widget_style_material_you,
        NomaWidgetStyle.FROSTED_GREEN to R.string.widget_style_frosted_green,
        NomaWidgetStyle.LIQUID_GLASS to R.string.widget_style_liquid_glass
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_style_settings)) },
        text = {
            Column(Modifier.selectableGroup().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                styles.forEach { (style, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .selectable(
                                selected = selected == style,
                                role = Role.RadioButton,
                                onClick = { onSelect(style) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == style, onClick = null)
                        Spacer(Modifier.width(NomaSpacing.small))
                        val previewBrush = when (style) {
                            NomaWidgetStyle.MATERIAL_YOU -> Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                            )
                            NomaWidgetStyle.FROSTED_GREEN -> Brush.linearGradient(
                                listOf(Color(0xFFD7EADD), Color(0xFF9FC8AB), Color(0xFFDBEDE2))
                            )
                            NomaWidgetStyle.LIQUID_GLASS -> Brush.linearGradient(
                                listOf(Color(0xFFFBFEFF), Color(0xFFBDD6E9), Color(0xFFE6F6FF))
                            )
                        }
                        Box(
                            modifier = Modifier.size(width = 52.dp, height = 32.dp)
                                .background(previewBrush, RoundedCornerShape(12.dp))
                                .padding(2.dp)
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(NomaSpacing.comfortable))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(NomaSpacing.small))
                Text(
                    stringResource(R.string.widget_style_glass_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun DeleteConfirmation(noteTitle: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_body_named, noteTitle.take(80))) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete_note),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun CenterMessage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

private fun formatEdited(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

private fun shareNote(context: Context, note: Note) {
    val text = listOf(note.title, note.body)
        .filter(String::isNotBlank)
        .joinToString(separator = "\n\n")
        .ifBlank { context.getString(R.string.untitled) }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_note)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.share_unavailable, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.share_unavailable, Toast.LENGTH_SHORT).show()
    }
}
