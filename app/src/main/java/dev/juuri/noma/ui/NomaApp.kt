package dev.juuri.noma.ui

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BrightnessAuto
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.juuri.noma.ui.theme.nomaColorScheme
import dev.juuri.noma.ui.theme.isNomaDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import dev.juuri.noma.R
import dev.juuri.noma.capture.NomaWidgetStyle
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.domain.NoteSearch
import dev.juuri.noma.domain.NoteFindReplace
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.ui.theme.NomaAppearance
import dev.juuri.noma.ui.theme.NomaAccent
import dev.juuri.noma.ui.theme.NomaThemeFamily
import dev.juuri.noma.ui.theme.resolveNomaColorScheme
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
    accent: NomaAccent = NomaAccent.FOREST,
    family: NomaThemeFamily = NomaThemeFamily.ACCENT_WHITE,
    onFamilyChange: (NomaThemeFamily) -> Unit = {},
    onAccentChange: (NomaAccent) -> Unit = {},
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
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.incoming_share_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                    Text(stringResource(R.string.incoming_share_body))
                    if (incoming.title.isNotEmpty()) {
                        Text(
                            incoming.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("noma_share_preview"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            incoming.body.take(500),
                            modifier = Modifier.padding(NomaSpacing.comfortable),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (storageState !is NotesSnapshot.Ready) {
                        Text(
                            stringResource(R.string.notes_storage_not_ready),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("noma_share_storage_guidance")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                    if (invalidShareReason != null) {
                        Text(
                            invalidShareReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("noma_share_validation_error")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                    if (active != null && saveState != EditorSaveState.Saved) {
                        Text(
                            shareNeedsSave,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("noma_share_save_guidance")
                                .semantics { liveRegion = LiveRegionMode.Polite }
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
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        .testTag("noma_share_confirm")
                ) {
                    if (capturingShare) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).testTag("noma_share_progress"),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(NomaSpacing.small))
                        Text(stringResource(R.string.incoming_share_saving))
                    } else {
                        Text(stringResource(R.string.incoming_share_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !capturingShare,
                    onClick = viewModel::dismissSharedText,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        .testTag("noma_share_discard")
                ) { Text(stringResource(R.string.incoming_share_discard)) }
            }
        )
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = shouldShowNomaTwoPane(maxWidth)
            if (expanded) {
                val gutter = nomaPaneGutter(maxWidth)
                val margin = nomaTabletMargin(maxWidth)
                val cornerRadius = nomaPaneCornerRadius(maxWidth)
                val listWidth by animateDpAsState(
                    targetValue = responsiveListPaneWidth(maxWidth),
                    animationSpec = NomaMotion.spatialFast(),
                    label = "nomaResponsiveListPaneWidth"
                )
                Row(
                    Modifier.fillMaxSize().padding(margin),
                    horizontalArrangement = Arrangement.spacedBy(gutter)
                ) {
                    Surface(
                        modifier = Modifier.width(listWidth).fillMaxHeight(),
                        shape = RoundedCornerShape(cornerRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp
                    ) {
                        NotesListScreen(
                            viewModel = viewModel,
                            snackbar = snackbar,
                            selectedId = active?.id,
                            appearance = appearance,
                            onAppearanceChange = onAppearanceChange,
                            accent = accent,
                            family = family,
                            onFamilyChange = onFamilyChange,
                            onAccentChange = onAccentChange,
                            widgetStyle = widgetStyle,
                            onWidgetStyleChange = onWidgetStyleChange,
                            isTabletList = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(cornerRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp
                    ) {
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
                        accent = accent,
                        family = family,
                        onFamilyChange = onFamilyChange,
                        onAccentChange = onAccentChange,
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


/**
 * Keep list navigation comfortably usable while preserving writing space on
 * tablets and resizable desktop windows. Only expanded layouts use this width.
 */
internal fun shouldShowNomaTwoPane(windowWidth: Dp): Boolean =
    windowWidth >= 840.dp

/** Avoid costly placement animations when a filtered notebook has many cards. */
internal fun shouldAnimateNomaListItems(noteCount: Int): Boolean =
    noteCount in 0..80

/** Keep long theme-family labels readable instead of crushing two cards on narrow windows. */
internal fun nomaThemeFamilyColumns(availableWidth: Dp, fontScale: Float): Int =
    if (availableWidth < 380.dp || fontScale >= 1.3f) 1 else 2

internal fun nomaPaneGutter(windowWidth: Dp): Dp =
    if (windowWidth >= 1200.dp) 16.dp else 8.dp

internal fun nomaTabletMargin(windowWidth: Dp): Dp =
    if (windowWidth >= 1200.dp) 16.dp else 8.dp

internal fun nomaPaneCornerRadius(windowWidth: Dp): Dp =
    if (windowWidth >= 1200.dp) 28.dp else 22.dp

internal fun responsiveListPaneWidth(windowWidth: Dp): Dp =
    (windowWidth * 0.36f).coerceIn(320.dp, 420.dp)

@Composable
private fun NotesListScreen(
    viewModel: NotesViewModel,
    snackbar: SnackbarHostState,
    selectedId: String?,
    appearance: NomaAppearance,
    onAppearanceChange: (NomaAppearance) -> Unit,
    accent: NomaAccent,
    family: NomaThemeFamily,
    onFamilyChange: (NomaThemeFamily) -> Unit,
    onAccentChange: (NomaAccent) -> Unit,
    widgetStyle: NomaWidgetStyle,
    onWidgetStyleChange: (NomaWidgetStyle) -> Unit,
    isTabletList: Boolean = false,
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
    // Limit recompositions while the user drags through long notebooks.
    val isDeepInList by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex >= 4 }
    }
    val listScrollScope = rememberCoroutineScope()
    var returningToTop by remember { mutableStateOf(false) }
    LaunchedEffect(query, pinnedOnly, archivedOnly, sort) {
        // A filter can collapse the list to zero rows; reset its previous scroll
        // position so returning results never starts at a stale offset.
        listState.scrollToItem(0)
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val searchKeyboard = LocalSoftwareKeyboardController.current
    val portableScope = rememberCoroutineScope()
    val searchAccessibilityLabel = stringResource(R.string.search_hint)
    val contextualSearchHint = stringResource(
        when {
            archivedOnly -> R.string.search_archived_hint
            pinnedOnly -> R.string.search_pinned_hint
            else -> R.string.search_hint
        }
    )
    val resultCountLabel = if (snapshot is NotesSnapshot.Ready && query.isNotBlank()) {
        val count = filteredNotes?.size ?: 0
        pluralStringResource(R.plurals.search_results_total, count, count)
    } else null

    var deleting by remember { mutableStateOf<Note?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var showWidgetStyles by rememberSaveable { mutableStateOf(false) }

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
            selectedAccent = accent,
            selectedFamily = family,
            onFamilySelect = onFamilyChange,
            onAccentSelect = onAccentChange,
            onSelect = {
                onAppearanceChange(it)
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
            },
            onDismiss = { showWidgetStyles = false; showAppearance = true }
        )
    }

    val newNoteLabel = stringResource(R.string.new_note)
    val creatingNoteLabel = stringResource(R.string.creating_note)
    val creatingOrCapturing = creating || capturingShare
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val sortButtonBackground by animateColorAsState(
        targetValue = if (sort == NoteSort.RECENT) Color.Transparent
            else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaSortButtonSurface"
    )
    val currentSortLabel = stringResource(if (sort == NoteSort.RECENT) R.string.sort_recent else R.string.sort_title)
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
    )
    Scaffold(
        modifier = modifier,
        containerColor = if (isTabletList) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.background,
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
                        tween(NomaMotion.quick)
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
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .testTag("noma_clear_filters"),
                        onClick = {
                            // Commit clear before releasing IME focus to avoid
                            // a late composing-text callback restoring the query.
                            viewModel.clearDiscoveryFilters()
                            focusManager.clearFocus()
                            searchKeyboard?.hide()
                        }
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(NomaSpacing.small))
                        Text(stringResource(R.string.clear_all_filters))
                    }
                }
                AnimatedVisibility(
                    visible = isDeepInList && !imeVisible &&
                        snapshot is NotesSnapshot.Ready && !filteredNotes.isNullOrEmpty(),
                    enter = fadeIn(tween(NomaMotion.quick)) + expandVertically(),
                    exit = fadeOut(tween(NomaMotion.quick)) + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                            .testTag("noma_scroll_to_top_row"),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            enabled = !returningToTop,
                            onClick = {
                                if (returningToTop) return@TextButton
                                returningToTop = true
                                sortMenuExpanded = false
                                listScrollScope.launch {
                                    // Avoid animating across hundreds of composed rows.
                                    if (listState.firstVisibleItemIndex > 40) {
                                        listState.scrollToItem(12)
                                    }
                                    try {
                                        listState.animateScrollToItem(0)
                                    } finally {
                                        returningToTop = false
                                    }
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape
                                )
                                .testTag("noma_scroll_to_top")
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(NomaSpacing.tiny))
                            Text(
                                stringResource(R.string.scroll_to_top),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                if (resultCountLabel != null) {
                    Text(
                        resultCountLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(start = NomaSpacing.medium, bottom = NomaSpacing.small)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("noma_search_result_count")
                    )
                }
                // One thumb-reachable row: search and capture share the same dock.
                // This replaces the FAB stacked above the search field.
                Row(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                        .testTag("noma_bottom_dock"),
                    horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomSearchPill(
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        enabled = snapshot is NotesSnapshot.Ready,
                        accessibilityLabel = searchAccessibilityLabel,
                        hint = contextualSearchHint,
                        onSearchFocus = {
                            sortMenuExpanded = false
                            showAppearance = false
                            showWidgetStyles = false
                        },
                        onSearchDone = {
                            focusManager.clearFocus()
                            searchKeyboard?.hide()
                        },
                        onDismiss = {
                            focusManager.clearFocus()
                            searchKeyboard?.hide()
                        },
                        // Preserve input focus when clearing so the next search
                        // can be typed without dismissing and reopening the IME.
                        onClear = {
                            sortMenuExpanded = false
                            viewModel.onQueryChange("")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    // Capture stays available with a search keyboard open.
                    // Busy and storage-not-ready cases preserve existing safeguards.
                    if (snapshot is NotesSnapshot.Ready) {
                        FloatingActionButton(
                            onClick = {
                                if (!creatingOrCapturing) {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    sortMenuExpanded = false
                                    viewModel.createNote()
                                }
                            },
                            modifier = Modifier.size(if (imeVisible) 48.dp else 56.dp)
                                .testTag("noma_new_note")
                                .semantics {
                                    contentDescription = if (creatingOrCapturing) creatingNoteLabel
                                        else newNoteLabel
                                    if (creatingOrCapturing) disabled()
                                },
                            shape = CircleShape,
                            containerColor = if (creatingOrCapturing)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            contentColor = if (creatingOrCapturing)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
                        ) {
                            if (creatingOrCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp)
                                        .testTag("noma_creation_spinner"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NomaSpacing.page, end = NomaSpacing.page, top = NomaSpacing.small, bottom = NomaSpacing.tiny)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f).animateContentSize(
                            tween(NomaMotion.quick)
                        )
                    ) {
                        Text(
                            if (archivedOnly) stringResource(R.string.archived_section)
                            else stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        AnimatedVisibility(
                            visible = !isDeepInList && !imeVisible,
                            enter = fadeIn(tween(NomaMotion.standard)) + expandVertically(),
                            exit = fadeOut(tween(NomaMotion.standard)) + shrinkVertically()
                        ) {
                            Text(
                                stringResource(R.string.library_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            sortMenuExpanded = false
                            focusManager.clearFocus()
                            searchKeyboard?.hide()
                            showAppearance = true
                        },
                        modifier = Modifier.defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                    ) {
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(NomaSpacing.tiny),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                        FilterChip(
                            selected = !archivedOnly && !pinnedOnly,
                            onClick = {
                                focusManager.clearFocus()
                                searchKeyboard?.hide()
                                sortMenuExpanded = false
                                // Restore the main library while preserving the typed query.
                                when {
                                    archivedOnly -> viewModel.toggleArchiveView()
                                    pinnedOnly -> viewModel.togglePinnedOnly()
                                }
                            },
                            enabled = snapshot is NotesSnapshot.Ready,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                .testTag("noma_all_filter"),
                            label = { Text(stringResource(R.string.all_notes_filter)) },
                            shape = CircleShape,
                            colors = chipColors
                        )
                        if (!archivedOnly) {
                            FilterChip(
                                selected = pinnedOnly,
                                onClick = {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    sortMenuExpanded = false
                                    viewModel.togglePinnedOnly()
                                },
                                enabled = snapshot is NotesSnapshot.Ready,
                                modifier = Modifier.testTag("noma_pinned_filter")
                                    .defaultMinSize(minHeight = 48.dp),
                                label = { Text(stringResource(R.string.pinned_filter)) },
                                shape = CircleShape,
                                colors = chipColors
                            )
                        }
                        FilterChip(
                            selected = archivedOnly,
                            onClick = {
                                focusManager.clearFocus()
                                searchKeyboard?.hide()
                                sortMenuExpanded = false
                                viewModel.toggleArchiveView()
                            },
                            enabled = snapshot is NotesSnapshot.Ready,
                            modifier = Modifier.testTag("noma_archive_filter")
                                .defaultMinSize(minHeight = 48.dp),
                            label = { Text(stringResource(R.string.archive_view)) },
                            shape = CircleShape,
                            colors = chipColors
                        )
                    Box {
                        IconButton(
                            onClick = {
                                if (!showAppearance && !showWidgetStyles) {
                                    focusManager.clearFocus()
                                    searchKeyboard?.hide()
                                    sortMenuExpanded = true
                                }
                            },
                            enabled = snapshot is NotesSnapshot.Ready,
                            modifier = Modifier
                                .defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                                .background(sortButtonBackground, CircleShape)
                                .testTag("noma_sort_button")
                                .semantics { stateDescription = currentSortLabel }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = stringResource(R.string.sort_notes),
                                tint = if (sort == NoteSort.RECENT)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_recent)) },
                                modifier = Modifier.semantics {
                                    selected = sort == NoteSort.RECENT
                                    stateDescription = currentSortLabel
                                },
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
                    val count = remember(snapshot, archivedOnly) {
                        (snapshot as NotesSnapshot.Ready).entries.count {
                            it.isArchived == archivedOnly
                        }
                    }
                    val filteredCount = filteredNotes?.size ?: 0
                    if (query.isBlank()) {
                        Text(
                            text = if (pinnedOnly)
                                pluralStringResource(R.plurals.search_results_total, filteredCount, filteredCount)
                            else pluralStringResource(R.plurals.note_total, count, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
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
                    Text(
                        stringResource(R.string.unavailable),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(NomaSpacing.small))
                    Text(
                        stringResource(R.string.error_recovery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(NomaSpacing.comfortable))
                    OutlinedButton(
                        onClick = viewModel::retry,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .testTag("noma_storage_retry")
                    ) { Text(stringResource(R.string.retry)) }
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
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() },
                                textAlign = TextAlign.Center
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val animateListItems = shouldAnimateNomaListItems(visible.size)
                        // The partition only changes when visible note data or archive
                        // scope changes, not on every animation frame or scroll tick.
                        val (pinned, others) = remember(visible, archivedOnly) {
                            if (archivedOnly) emptyList<Note>() to visible
                            else visible.partition { it.isPinned }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("noma_note_list"),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = NomaSpacing.comfortable,
                                end = NomaSpacing.comfortable,
                                bottom = NomaSpacing.comfortable
                            ),
                            verticalArrangement = Arrangement.spacedBy(NomaSpacing.small)
                        ) {
                            if (pinned.isNotEmpty()) {
                                item(key = "pinned_section", contentType = "section_heading") {
                                    SectionHeader(
                                        title = stringResource(R.string.pinned_section),
                                        count = pinned.size,
                                        icon = Icons.Outlined.PushPin,
                                        showBadge = others.isNotEmpty()
                                    )
                                }
                                items(pinned, key = { it.id }, contentType = { "note_card" }) { note ->
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
                                        modifier = if (animateListItems) Modifier.animateItem(
                                            placementSpec = NomaMotion.spatialDefault()
                                        )
                                            else Modifier
                                    )
                                }
                            }
                            if (others.isNotEmpty()) {
                                item(key = "recent_section", contentType = "section_heading") {
                                    SectionHeader(
                                        title = stringResource(
                                            when {
                                                archivedOnly -> R.string.archived_section
                                                query.isNotBlank() -> R.string.search_section
                                                sort == NoteSort.TITLE -> R.string.alphabetical_section
                                                else -> R.string.recent_section
                                            }
                                        ),
                                        count = others.size,
                                        showBadge = pinned.isNotEmpty(),
                                        icon = when {
                                            archivedOnly -> Icons.Outlined.Archive
                                            query.isNotBlank() -> Icons.Outlined.Search
                                            sort == NoteSort.TITLE -> Icons.AutoMirrored.Outlined.Sort
                                            else -> Icons.Outlined.EditNote
                                        }
                                    )
                                }
                                items(others, key = { it.id }, contentType = { "note_card" }) { note ->
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
                                        modifier = if (animateListItems) Modifier.animateItem(
                                            placementSpec = NomaMotion.spatialDefault()
                                        )
                                            else Modifier
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
    hint: String,
    onSearchFocus: () -> Unit,
    onSearchDone: () -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (focused) 6.dp else 2.dp,
        animationSpec = tween(NomaMotion.quick),
        label = "nomaBottomSearchElevation"
    )
    val pillColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaBottomSearchSurface"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaBottomSearchStroke"
    )
    Row(
        modifier = modifier.fillMaxWidth().animateContentSize(
            tween(NomaMotion.quick)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f).testTag("noma_search_pill"),
            shape = CircleShape,
            color = pillColor,
            tonalElevation = if (focused) 3.dp else 2.dp,
            shadowElevation = elevation,
            border = BorderStroke(1.dp, strokeColor)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 58.dp)
                    .onFocusChanged {
                        if (it.isFocused && !focused) onSearchFocus()
                        focused = it.isFocused
                    }
                    .semantics { contentDescription = accessibilityLabel }
                    .testTag("noma_search"),
                singleLine = true,
                shape = CircleShape,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (focused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        AnimatedVisibility(
            visible = focused && enabled,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    .testTag("noma_search_dismiss")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, icon: ImageVector, showBadge: Boolean = true) {
    val accessibleCount = stringResource(
        R.string.section_count_description, title,
        pluralStringResource(R.plurals.note_total, count, count)
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = NomaSpacing.tiny, top = NomaSpacing.small, bottom = NomaSpacing.tiny)
            .testTag("noma_section_header")
            .clearAndSetSemantics { heading(); contentDescription = accessibleCount },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // The library already displays the total above the list. Only add
        // per-section badges when pinned and unpinned groups both exist.
        if (showBadge) Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = NomaSpacing.medium, vertical = NomaSpacing.tiny),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun highlightMatches(value: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val accent = MaterialTheme.colorScheme.onPrimaryContainer
    val background = MaterialTheme.colorScheme.primaryContainer
    return remember(value, query, accent, background) {
        buildAnnotatedString {
            append(value)
            NoteSearch.matchRanges(value, query).forEach { range ->
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = accent, background = background),
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
    LaunchedEffect(navigationEnabled, note.isArchived) {
        // A disabled card or a card moved into the archive must not keep
        // an old contextual menu attached to its previous location.
        if (!navigationEnabled || note.isArchived) menuExpanded = false
    }
    val fallbackTitle = stringResource(R.string.untitled)
    val title = remember(note.title, note.body, fallbackTitle) {
        note.displayTitle.ifBlank { fallbackTitle }
    }
    val localeKey = LocalConfiguration.current.locales.toLanguageTags()
    val editedTimestamp = remember(note.updatedAt, localeKey) { formatEdited(note.updatedAt) }
    val pinnedBadgeVisible = note.isPinned && !note.isArchived
    val relevantPreview = remember(note.id, note.title, note.body, query) {
        NoteSearch.matchingPreview(note, query)
    }
    val cardColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaCardSelectedSurface"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaCardSelectionBorder"
    )
    val cornerRadius = 16.dp
    val cardElevation = 0.dp
    Card(
        onClick = onOpen,
        enabled = navigationEnabled,
        modifier = modifier.fillMaxWidth()
            .testTag("noma_note_${note.id}")
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = if (selected) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(start = NomaSpacing.comfortable, top = NomaSpacing.small, end = NomaSpacing.tiny, bottom = NomaSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = highlightMatches(title, query),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (relevantPreview.isNotEmpty() &&
                    (note.title.isNotBlank() || relevantPreview != note.displayTitle)) {
                    Spacer(Modifier.height(NomaSpacing.tiny))
                    Text(
                        text = highlightMatches(relevantPreview, query),
                        maxLines = if (query.isBlank()) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(NomaSpacing.tiny))
                Text(
                    text = editedTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (pinnedBadgeVisible) {
                Surface(
                    modifier = Modifier.testTag("noma_card_pinned_badge"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.pinned_note_indicator),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                }
            }
            if (note.isArchived) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = stringResource(R.string.archived_section),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("noma_card_archived_badge")
                        .padding(start = NomaSpacing.small).size(18.dp)
                )
            }
            Box {
                IconButton(
                    onClick = {
                        if (navigationEnabled) {
                            onMenuOpen()
                            menuExpanded = true
                        }
                    },
                    enabled = navigationEnabled,
                    modifier = Modifier.defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                        .testTag("noma_note_actions_${note.id}")
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
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("noma_menu_pin"),
                text = { Text(stringResource(if (note.isPinned) R.string.unpin_note else R.string.pin_note)) },
                enabled = destructiveEnabled,
                onClick = { onDismiss(); onPin() },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = if (note.isPinned && destructiveEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (note.isPinned && destructiveEnabled) {
                    { Icon(Icons.Outlined.Check, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary) }
                } else null
            )
        }
        DropdownMenuItem(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("noma_menu_archive"),
            text = { Text(stringResource(if (note.isArchived) R.string.unarchive_note else R.string.archive_note)) },
            enabled = destructiveEnabled,
            onClick = { onDismiss(); onArchive() },
            leadingIcon = {
                Icon(
                    if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        DropdownMenuItem(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("noma_menu_share"),
            text = { Text(stringResource(R.string.share_note)) },
            enabled = destructiveEnabled && (note.title.isNotBlank() || note.body.isNotBlank()),
            onClick = { onDismiss(); onShare() },
            leadingIcon = {
                Icon(Icons.Outlined.Share, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
        // Give the destructive action its own group without a loud accent color.
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = NomaSpacing.medium, vertical = NomaSpacing.tiny),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        DropdownMenuItem(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                .testTag("noma_delete_action"),
            text = {
                Text(
                    stringResource(R.string.delete_note),
                    color = if (destructiveEnabled) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            enabled = destructiveEnabled,
            onClick = { onDismiss(); onDelete() },
            leadingIcon = {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = if (destructiveEnabled) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    var findReplaceOpen by rememberSaveable(note.id) { mutableStateOf(false) }
    var focusBodyAfterFind by remember(note.id) { mutableStateOf(false) }
    var findQuery by rememberSaveable(note.id) { mutableStateOf("") }
    var replaceWith by rememberSaveable(note.id) { mutableStateOf("") }
    var closeWhenSaved by rememberSaveable(note.id) { mutableStateOf(false) }
    var bodyValue by rememberSaveable(note.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note.body))
    }
    // External replacements use the same document; keep the caret clamped to it.
    val currentBody = if (bodyValue.text == note.body) bodyValue else bodyValue.copy(text = note.body)
    val context = LocalContext.current
    val titleFocus = remember(note.id) { FocusRequester() }
    val bodyFocus = remember(note.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val saveBarColor by animateColorAsState(
        targetValue = if (saveState is EditorSaveState.Failed) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(NomaMotion.standard),
        label = "nomaEditorSaveStatusSurface"
    )
    val editorScrollState = rememberSaveable(note.id, saver = ScrollState.Saver) { ScrollState(0) }
    LaunchedEffect(findReplaceOpen, focusBodyAfterFind) {
        if (!findReplaceOpen && focusBodyAfterFind) {
            bodyFocus.requestFocus()
            // Return directly to editing the selected result; dismissing the
            // search dialog must not unexpectedly hide the writing keyboard.
            keyboard?.show()
            focusBodyAfterFind = false
        }
    }
    val titleFieldLabel = stringResource(R.string.title_hint)
    val bodyFieldLabel = stringResource(R.string.body_accessibility_label)

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
            shape = MaterialTheme.shapes.extraLarge,
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
                FlowRow(horizontalArrangement = Arrangement.End) {
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
    if (findReplaceOpen) {
        val matches = remember(note.body, findQuery) {
            NoteFindReplace.count(note.body, findQuery)
        }
        val findFocus = remember { FocusRequester() }
        val selectedRange = currentBody.selection
        val currentOccurrence = if (findQuery.isNotBlank() && selectedRange.min < note.body.length &&
            note.body.regionMatches(selectedRange.min, findQuery, 0, findQuery.length, ignoreCase = true))
            NoteFindReplace.count(note.body.substring(0, selectedRange.min), findQuery) + 1 else 0
        fun navigate(backwards: Boolean) {
            val from = if (backwards) currentBody.selection.min - 1 else currentBody.selection.max
            val range = NoteFindReplace.nextMatch(note.body, findQuery, from, backwards) ?: return
            bodyValue = TextFieldValue(note.body, TextRange(range.first, range.last + 1))
            findReplaceOpen = false
            focusBodyAfterFind = true
        }
        AlertDialog(
            onDismissRequest = { findReplaceOpen = false },
            title = { Text(stringResource(R.string.find_replace)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                    LaunchedEffect(Unit) { findFocus.requestFocus() }
                    TextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.find_text)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { navigate(false) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(findFocus).testTag("noma_find_input")
                    )
                    TextField(
                        value = replaceWith,
                        onValueChange = { replaceWith = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.replacement_text)) },
                        modifier = Modifier.fillMaxWidth().testTag("noma_replace_input")
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { navigate(true) }, enabled = matches > 0,
                            modifier = Modifier.testTag("noma_find_previous")) {
                            Text(stringResource(R.string.previous_match))
                        }
                        TextButton(onClick = { navigate(false) }, enabled = matches > 0,
                            modifier = Modifier.testTag("noma_find_next")) {
                            Text(stringResource(R.string.next_match))
                        }
                    }
                    Text(
                        text = if (matches > 0) stringResource(R.string.match_position, currentOccurrence, matches)
                            else stringResource(R.string.no_search_matches),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("noma_find_result_count")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(NomaSpacing.tiny)
                ) {
                    TextButton(
                        enabled = matches > 0,
                        onClick = {
                            closeWhenSaved = false
                            val updated = NoteFindReplace.replaceFirst(note.body, findQuery, replaceWith)
                            val replacedAt = note.body.indexOf(findQuery, ignoreCase = true)
                            val range = NoteFindReplace.nextMatch(updated, findQuery,
                                replacedAt.coerceAtLeast(0) + replaceWith.length)
                            bodyValue = TextFieldValue(updated, TextRange(range?.first ?: 0))
                            viewModel.changeBody(updated)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("noma_replace_first")
                            .defaultMinSize(minHeight = 48.dp)
                    ) { Text(stringResource(R.string.replace_first)) }
                    TextButton(
                        enabled = matches > 0,
                        onClick = {
                            closeWhenSaved = false
                            val updated = NoteFindReplace.replaceAll(note.body, findQuery, replaceWith)
                            val range = NoteFindReplace.nextMatch(updated, findQuery, 0)
                            bodyValue = TextFieldValue(updated, TextRange(range?.first ?: 0))
                            viewModel.changeBody(updated)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("noma_replace_all")
                            .defaultMinSize(minHeight = 48.dp)
                    ) { Text(stringResource(R.string.replace_all)) }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { findReplaceOpen = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        .testTag("noma_find_replace_close")
                ) { Text(stringResource(R.string.find_replace_close)) }
            }
        )
    }
    val safeTitle = stringResource(R.string.editor_nav_title)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = if (!expanded) ({ SnackbarHost(snackbar) }) else ({ }),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
                        .padding(horizontal = NomaSpacing.small)
                        .testTag("noma_editor_toolbar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val exportScope = rememberCoroutineScope()
                    // Document export stays available when reading, but does not compete
                    // with the writing tools or Gboard while the IME is visible.
                    if (!keyboardVisible) {
                        PortableNoteExport(note, enabled = saveState == EditorSaveState.Saved, onMessage = { message ->
                            exportScope.launch { snackbar.showSnackbar(message) }
                        })
                    }
                        IconButton(
                            onClick = {
                                menuExpanded = false
                                findReplaceOpen = true
                            },
                            modifier = Modifier.defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                                .testTag("noma_editor_find_replace")
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.find_replace)
                            )
                        }
                    IconButton(
                        onClick = {
                            val edit = toggleBulletLine(currentBody)
                            bodyValue = edit
                            closeWhenSaved = false
                            viewModel.changeBody(edit.text)
                            bodyFocus.requestFocus()
                            keyboard?.show()
                        },
                        modifier = Modifier.testTag("noma_editor_bullet")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = stringResource(R.string.toggle_bullet))
                    }
                    if (keyboardVisible) {
                        IconButton(onClick = { keyboard?.hide() },
                            modifier = Modifier.testTag("noma_editor_hide_keyboard")) {
                            Icon(Icons.Outlined.KeyboardHide,
                                contentDescription = stringResource(R.string.hide_keyboard))
                        }
                    }
                }
            }
        },
        topBar = {
            Column(Modifier.animateContentSize(NomaMotion.spatialGentle())) {
                TopAppBar(
                    title = {
                        Text(
                            if (expanded) note.displayTitle.ifBlank { stringResource(R.string.untitled) } else safeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = ::requestClose,
                            modifier = Modifier.defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                                .testTag("noma_editor_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_to_notes)
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.defaultMinSize(minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget)
                                    .testTag("noma_editor_actions")
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
                                destructiveEnabled = saveState == EditorSaveState.Saved
                            )
                        }
                    }
                )
                // A save failure must stay actionable even while the editor has
                // scrolled and the IME is visible; never bury Retry in the document.
                if (saveState != EditorSaveState.Saved) Surface(
                    color = saveBarColor,
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
                AnimatedVisibility(
                    visible = saveState is EditorSaveState.Failed,
                    enter = fadeIn(tween(NomaMotion.quick)) + expandVertically(),
                    exit = fadeOut(tween(NomaMotion.quick)) + shrinkVertically()
                ) {
                    Text(
                        stringResource(R.string.save_failure_guidance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = NomaSpacing.page, vertical = NomaSpacing.small)
                            .testTag("noma_save_failure_guidance")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (closeWhenSaved && saveState == EditorSaveState.Saving) {
                    Text(
                        stringResource(R.string.wait_for_save_before_exit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = NomaSpacing.page, vertical = NomaSpacing.small)
                            .testTag("noma_pending_exit")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        }
    ) { insets ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxReadingWidth = NomaSpacing.editorMaxWidth
            val width = if (maxWidth > maxReadingWidth) maxReadingWidth else maxWidth
            val writingHeight = (maxHeight - 160.dp).coerceAtLeast(48.dp)
            Column(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .verticalScroll(editorScrollState)
                    .padding(start = NomaSpacing.page, end = NomaSpacing.page,
                        top = NomaSpacing.small, bottom = NomaSpacing.section)
            ) {
                Spacer(Modifier.height(NomaSpacing.small))
                BasicTextField(
                    value = note.title,
                    onValueChange = {
                        closeWhenSaved = false
                        viewModel.changeTitle(it)
                    },
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = NomaSpacing.touchTarget)
                        .focusRequester(titleFocus)
                        .semantics { contentDescription = titleFieldLabel }
                        .testTag("noma_title_input"),
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
                // Preserve a continuous writing surface. Metadata is read-only
                // and never participates in the editable or exported note text.
                if (!keyboardVisible && (note.title.isNotBlank() || note.body.isNotBlank())) {
                    Spacer(Modifier.height(NomaSpacing.small))
                    Text(
                        text = stringResource(
                            R.string.last_edited_at,
                            remember(note.updatedAt, LocalConfiguration.current.locales.toLanguageTags()) { formatEdited(note.updatedAt) }
                        ),
                        modifier = Modifier.testTag("noma_editor_metadata"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Editorial rhythm: keep the writing area close to its title.
                Spacer(Modifier.height(NomaSpacing.medium))
                BasicTextField(
                    value = currentBody,
                    onValueChange = {
                        bodyValue = it
                        if (it.text != note.body) {
                            closeWhenSaved = false
                            viewModel.changeBody(it.text)
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 27.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                        .defaultMinSize(minHeight = writingHeight)
                        .focusRequester(bodyFocus)
                        .semantics { contentDescription = bodyFieldLabel }
                        .testTag("noma_body_input"),
                    decorationBox = { input ->
                        Box {
                            if (note.body.isEmpty()) {
                                Text(
                                    stringResource(R.string.body_hint),
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
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
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onRetry,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("noma_retry_save")) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorPane() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(NomaSpacing.page)
                .testTag("noma_tablet_empty_pane"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NomaSpacing.medium)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(NomaSpacing.comfortable).size(36.dp)
                )
            }
            Text(
                stringResource(R.string.select_note_title),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.select_note_detail),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceDialog(
    selected: NomaAppearance,
    selectedAccent: NomaAccent,
    selectedFamily: NomaThemeFamily,
    onFamilySelect: (NomaThemeFamily) -> Unit,
    onAccentSelect: (NomaAccent) -> Unit,
    onSelect: (NomaAppearance) -> Unit,
    onDismiss: () -> Unit,
    onWidgetStyles: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("noma_appearance_page"),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.appearance)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close))
                        }
                    }
                )
            }
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(NomaSpacing.comfortable),
                    verticalArrangement = Arrangement.spacedBy(NomaSpacing.medium)
                ) {
                    Text(stringResource(R.string.theme_family_title), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columns = nomaThemeFamilyColumns(maxWidth, LocalDensity.current.fontScale)
                        Column(Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                            NomaThemeFamily.entries.chunked(columns).forEach { options ->
                            Row(horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                                options.forEach { option ->
                                    val label = stringResource(when (option) {
                                        NomaThemeFamily.BW_ACCENT -> R.string.theme_bw_accent
                                        NomaThemeFamily.BW_PURE -> R.string.theme_bw_pure
                                        NomaThemeFamily.MATERIAL_YOU -> R.string.theme_material_you
                                        NomaThemeFamily.ACCENT_WHITE -> R.string.theme_accent_white
                                    })
                                    val preview = resolveNomaColorScheme(isNomaDark(selected), selectedAccent, option)
                                    Surface(
                                        modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                                            .selectable(selected = selectedFamily == option, role = Role.RadioButton,
                                                onClick = { onFamilySelect(option) })
                                            .testTag("noma_family_${option.name}"),
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (selectedFamily == option) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (selectedFamily == option) 1.5.dp else 1.dp,
                                            if (selectedFamily == option) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(NomaSpacing.small),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)
                                        ) {
                                            Column(
                                                Modifier.size(width = 36.dp, height = 32.dp)
                                                    .background(preview.background, MaterialTheme.shapes.small)
                                                    .padding(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Box(Modifier.fillMaxWidth().height(4.dp)
                                                    .background(preview.onBackground, CircleShape))
                                                Box(Modifier.weight(1f).fillMaxWidth()
                                                    .background(preview.surfaceContainerHigh, MaterialTheme.shapes.extraSmall))
                                                Box(Modifier.fillMaxWidth().height(4.dp)
                                                    .background(preview.primary, CircleShape))
                                            }
                                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                                            if (selectedFamily == option) Icon(
                                                Icons.Outlined.Check, contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)) {
                        NomaAppearance.entries.forEach { mode ->
                            val checked = mode == selected
                            FilterChip(
                                selected = checked,
                                onClick = { onSelect(mode) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = NomaSpacing.touchTarget)
                                    .testTag("noma_theme_preview_${mode.name}"),
                                label = {
                                    Text(stringResource(when (mode) {
                                        NomaAppearance.SYSTEM -> R.string.appearance_system
                                        NomaAppearance.LIGHT -> R.string.appearance_light
                                        NomaAppearance.DARK -> R.string.appearance_dark
                                    }))
                                },
                                leadingIcon = if (checked) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                    if (selectedFamily == NomaThemeFamily.BW_ACCENT || selectedFamily == NomaThemeFamily.ACCENT_WHITE) Text(stringResource(R.string.accent_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = NomaSpacing.small).semantics { heading() })
                    if (selectedFamily == NomaThemeFamily.BW_ACCENT || selectedFamily == NomaThemeFamily.ACCENT_WHITE) {
                        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
                                NomaAccent.entries.forEach { swatch ->
                                    val checked = swatch == selectedAccent
                                    val label = stringResource(when (swatch) {
                                        NomaAccent.FOREST -> R.string.accent_forest
                                        NomaAccent.SAGE -> R.string.accent_sage
                                        NomaAccent.HONEY -> R.string.accent_honey
                                        NomaAccent.CORAL -> R.string.accent_coral
                                        NomaAccent.LILAC -> R.string.accent_lilac
                                        NomaAccent.SKY -> R.string.accent_sky
                                    })
                                    val colors = resolveNomaColorScheme(isNomaDark(selected), swatch, selectedFamily)
                                    Box(
                                        Modifier.size(NomaSpacing.touchTarget)
                                            .selectable(selected = checked, role = Role.RadioButton,
                                                onClick = { onAccentSelect(swatch) })
                                            .semantics { contentDescription = label }
                                            .testTag("noma_accent_${swatch.name}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            Modifier.size(28.dp)
                                                .background(colors.primary, CircleShape)
                                                .then(if (checked) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (checked) Icon(Icons.Outlined.Check, null,
                                                tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                        }
                    }
                    OutlinedButton(onClick = onWidgetStyles,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = NomaSpacing.touchTarget)) {
                        Text(stringResource(R.string.widget_style_settings))
                    }
                }
            }
        }
    }
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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.widget_style_settings)) },
        text = {
            Column(
                Modifier.selectableGroup().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NomaSpacing.tiny)
            ) {
                styles.forEach { (style, label) ->
                    val isSelected = selected == style
                    val choiceColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        animationSpec = tween(NomaMotion.standard),
                        label = "nomaWidgetChoiceBackground"
                    )
                    val choiceOutline by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(NomaMotion.standard),
                        label = "nomaWidgetChoiceStroke"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = NomaSpacing.tiny)
                            .background(choiceColor, RoundedCornerShape(20.dp))
                            .border(1.dp, choiceOutline, RoundedCornerShape(20.dp))
                            .defaultMinSize(minHeight = 76.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(style) }
                            )
                            .padding(horizontal = NomaSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(NomaSpacing.small))
                        val previewBrush = when (style) {
                            NomaWidgetStyle.MATERIAL_YOU -> Brush.linearGradient(
                                listOf(
                                    colorResource(R.color.noma_widget_material_background),
                                    colorResource(R.color.noma_widget_material_background)
                                )
                            )
                            NomaWidgetStyle.FROSTED_GREEN -> Brush.linearGradient(
                                listOf(
                                    colorResource(R.color.noma_widget_frosted_start),
                                    colorResource(R.color.noma_widget_frosted_center),
                                    colorResource(R.color.noma_widget_frosted_end)
                                )
                            )
                            NomaWidgetStyle.LIQUID_GLASS -> Brush.linearGradient(
                                listOf(
                                    colorResource(R.color.noma_widget_glass_fill),
                                    colorResource(R.color.noma_widget_glass_fill)
                                )
                            )
                        }
                        val previewOutline = colorResource(when (style) {
                            NomaWidgetStyle.MATERIAL_YOU -> R.color.noma_widget_material_outline
                            NomaWidgetStyle.FROSTED_GREEN -> R.color.noma_widget_frosted_stroke
                            NomaWidgetStyle.LIQUID_GLASS -> R.color.noma_widget_glass_outer_stroke
                        })
                        val previewText = colorResource(when (style) {
                            NomaWidgetStyle.MATERIAL_YOU -> R.color.noma_widget_material_text
                            NomaWidgetStyle.FROSTED_GREEN -> R.color.noma_widget_frosted_text
                            NomaWidgetStyle.LIQUID_GLASS -> R.color.noma_widget_glass_text
                        })
                        Box(
                            modifier = Modifier.size(width = 116.dp, height = 36.dp)
                                .background(previewBrush, RoundedCornerShape(15.dp))
                                .border(1.dp, previewOutline, RoundedCornerShape(15.dp))
                                .then(if (style == NomaWidgetStyle.LIQUID_GLASS)
                                    Modifier.padding(1.dp).border(1.dp,
                                        colorResource(R.color.noma_widget_glass_stroke), RoundedCornerShape(14.dp))
                                    else Modifier)
                                .testTag("noma_widget_preview_${style.name}"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.noma_launcher_foreground),
                                    contentDescription = null,
                                    modifier = Modifier.size(23.dp)
                                )
                                Text(
                                    stringResource(R.string.shortcut_new_note),
                                    color = previewText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(NomaSpacing.comfortable))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Text(
                                stringResource(
                                    when (style) {
                                        NomaWidgetStyle.MATERIAL_YOU -> R.string.widget_style_material_you_detail
                                        NomaWidgetStyle.FROSTED_GREEN -> R.string.widget_style_frosted_green_detail
                                        NomaWidgetStyle.LIQUID_GLASS -> R.string.widget_style_liquid_glass_detail
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(selected = isSelected, onClick = null)
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun DeleteConfirmation(noteTitle: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_body_named, noteTitle.take(80)),
            modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    .testTag("noma_confirm_delete")
            ) {
                Text(
                    stringResource(R.string.delete_note),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    .testTag("noma_cancel_delete")
            ) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun CenterMessage(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = NomaSpacing.section, vertical = NomaSpacing.comfortable),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
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
