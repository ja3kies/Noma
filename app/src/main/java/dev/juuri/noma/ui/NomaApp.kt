package dev.juuri.noma.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.SaveAlt
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
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import dev.juuri.noma.R
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.ChecklistSyntax
import dev.juuri.noma.domain.MarkdownEdit
import dev.juuri.noma.domain.MarkdownEdits
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.domain.NotesSnapshot
import dev.juuri.noma.transfer.NoteDocumentTransfer
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
    onAppearanceChange: (NomaAppearance) -> Unit = {}
) {
    val active by viewModel.active.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(R.string.deleted_message)
    val archived = stringResource(R.string.archived_message)
    val restored = stringResource(R.string.unarchived_message)
    val undo = stringResource(R.string.undo)
    val failurePrefix = stringResource(R.string.error_prefix)

    LaunchedEffect(viewModel, deleted, archived, restored, undo, failurePrefix) {
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
                is NoteUiEvent.Failure ->
                    snackbar.showSnackbar(failurePrefix + event.message, withDismissAction = true)
            }
        }
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
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val query by viewModel.query.collectAsState()
    val pinnedOnly by viewModel.pinnedOnly.collectAsState()
    val archivedOnly by viewModel.archivedOnly.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val creating by viewModel.creating.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var deleting by remember { mutableStateOf<Note?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var transferMenuExpanded by remember { mutableStateOf(false) }
    var confirmUnencryptedExport by remember { mutableStateOf(false) }
    var transferBusy by remember { mutableStateOf(false) }
    val transferScope = rememberCoroutineScope()
    val exportSuccess = stringResource(R.string.export_success)
    val importSuccess = stringResource(R.string.import_success)
    val transferError = stringResource(R.string.transfer_error)

    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && !transferBusy) {
            transferScope.launch {
                transferBusy = true
                val result = runCatching {
                    val notes = viewModel.exportSnapshot()
                    NoteDocumentTransfer.writeBackup(context, uri, notes)
                }
                transferBusy = false
                snackbar.showSnackbar(
                    result.fold({ exportSuccess }, { transferError + (it.localizedMessage ?: "") })
                )
            }
        }
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !transferBusy) {
            transferScope.launch {
                transferBusy = true
                val result = runCatching {
                    val notes = NoteDocumentTransfer.readNotes(context, uri)
                    viewModel.importNotes(notes)
                }
                transferBusy = false
                snackbar.showSnackbar(
                    result.fold(
                        { count -> "$importSuccess ($count)" },
                        { transferError + (it.localizedMessage ?: "") }
                    )
                )
            }
        }
    }

    if (confirmUnencryptedExport) {
        AlertDialog(
            onDismissRequest = { confirmUnencryptedExport = false },
            title = { Text(stringResource(R.string.backup_warning_title)) },
            text = { Text(stringResource(R.string.backup_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnencryptedExport = false
                    backupPicker.launch("Noma-backup.json")
                }) { Text(stringResource(R.string.export_backup)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnencryptedExport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deleting?.let { note ->
        DeleteConfirmation(
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
            onDismiss = { showAppearance = false }
        )
    }

    val newNoteLabel = stringResource(R.string.new_note)
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!creating) viewModel.createNote() },
                expanded = true,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(newNoteLabel) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.testTag("noma_new_note")
            )
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
                    Box {
                        IconButton(
                            onClick = { transferMenuExpanded = true },
                            enabled = !transferBusy
                        ) {
                            Icon(
                                Icons.Outlined.MoreHoriz,
                                contentDescription = stringResource(R.string.notebook_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = transferMenuExpanded,
                            onDismissRequest = { transferMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_backup)) },
                                onClick = {
                                    transferMenuExpanded = false
                                    confirmUnencryptedExport = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_notes)) },
                                onClick = {
                                    transferMenuExpanded = false
                                    importPicker.launch(
                                        arrayOf("application/json", "text/plain", "text/markdown")
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                }
                            )
                        }
                    }
                    if (transferBusy) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = { showAppearance = true }) {
                        Icon(Icons.Outlined.Palette, contentDescription = stringResource(R.string.appearance))
                    }
                }
                Spacer(Modifier.height(20.dp))
                TextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("noma_search"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NomaSpacing.small)
                    ) {
                        FilterChip(
                            selected = archivedOnly,
                            onClick = viewModel::toggleArchiveView,
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
                                onClick = viewModel::togglePinnedOnly,
                                label = { Text(stringResource(R.string.pinned_filter)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.PushPin, contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        Box {
                            FilterChip(
                                selected = tagFilter != null,
                                onClick = { tagMenuExpanded = true },
                                label = { Text(tagFilter ?: stringResource(R.string.tags_filter)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenu(
                                expanded = tagMenuExpanded,
                                onDismissRequest = { tagMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.all_tags)) },
                                    onClick = {
                                        viewModel.selectTag(null)
                                        tagMenuExpanded = false
                                    }
                                )
                                val ready = snapshot as? NotesSnapshot.Ready
                                ready?.let { state ->
                                    viewModel.tagsFor(state.entries, archivedOnly).forEach { tag ->
                                        DropdownMenuItem(
                                            text = { Text("#$tag") },
                                            onClick = {
                                                viewModel.selectTag(tag)
                                                tagMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = stringResource(R.string.sort_notes))
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_recent)) },
                                onClick = {
                                    viewModel.selectSort(NoteSort.RECENT)
                                    sortMenuExpanded = false
                                },
                                trailingIcon = if (sort == NoteSort.RECENT) {
                                    { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title)) },
                                onClick = {
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
                    Text(
                        text = pluralStringResource(R.plurals.note_total, count, count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    val visible = remember(
                        state.entries, query, pinnedOnly, archivedOnly, sort, tagFilter
                    ) {
                        viewModel.visible(state.entries, query, pinnedOnly, archivedOnly, sort, tagFilter)
                    }
                    if (visible.isEmpty()) {
                        CenterMessage {
                            Icon(
                                when {
                                    query.isNotBlank() || pinnedOnly || tagFilter != null -> Icons.Outlined.Search
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
                                        query.isNotBlank() || pinnedOnly || tagFilter != null -> R.string.no_results
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
                                        query.isNotBlank() || pinnedOnly || tagFilter != null -> R.string.no_results_detail
                                        archivedOnly -> R.string.empty_archive_detail
                                        else -> R.string.empty_detail
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.entries.isEmpty()) {
                                Spacer(Modifier.height(NomaSpacing.section))
                                Button(onClick = viewModel::createNote) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(Modifier.width(NomaSpacing.small))
                                    Text(stringResource(R.string.new_note))
                                }
                            }
                        }
                    } else {
                        val pinned = if (archivedOnly) emptyList() else visible.filter { it.isPinned }
                        val others = if (archivedOnly) visible else visible.filterNot { it.isPinned }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("noma_note_list"),
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
                                        selected = note.id == selectedId,
                                        onOpen = { viewModel.openNote(note) },
                                        onPin = { viewModel.togglePin(note) },
                                        onArchive = { viewModel.toggleArchive(note) },
                                        onDelete = { deleting = note },
                                        onShare = { shareNote(context, note) }
                                    )
                                }
                            }
                            if (others.isNotEmpty()) {
                                item(key = "recent_section") {
                                    SectionHeader(
                                        stringResource(
                                            if (archivedOnly) R.string.archived_section
                                            else R.string.recent_section
                                        )
                                    )
                                }
                                items(others, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        selected = note.id == selectedId,
                                        onOpen = { viewModel.openNote(note) },
                                        onPin = { viewModel.togglePin(note) },
                                        onArchive = { viewModel.toggleArchive(note) },
                                        onDelete = { deleting = note },
                                        onShare = { shareNote(context, note) }
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
private fun NoteCard(
    note: Note,
    selected: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val title = note.displayTitle.ifBlank { stringResource(R.string.untitled) }
    val cardColor = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(NomaMotion.quick)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 15.dp, end = 6.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (note.preview.isNotEmpty() && note.title.isNotBlank()) {
                    Spacer(Modifier.height(NomaSpacing.tiny))
                    Text(
                        text = note.preview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (note.tags.isNotEmpty()) {
                    Spacer(Modifier.height(NomaSpacing.small))
                    Text(
                        text = note.tags.joinToString("  ·  ") { "#$it" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val taskItems = ChecklistSyntax.items(note.body)
                if (taskItems.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.checklist_progress,
                            taskItems.count { it.checked },
                            taskItems.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
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
                    contentDescription = stringResource(R.string.pin_note),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 3.dp).size(18.dp)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.note_actions))
                }
                NoteActionsMenu(
                    expanded = menuExpanded,
                    note = note,
                    onDismiss = { menuExpanded = false },
                    onPin = onPin,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onShare = onShare
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
    onExportMarkdown: (() -> Unit)? = null,
    onExportText: (() -> Unit)? = null
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(if (note.isPinned) R.string.unpin_note else R.string.pin_note)) },
            onClick = { onDismiss(); onPin() },
            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (note.isArchived) R.string.unarchive_note else R.string.archive_note)) },
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
            onClick = { onDismiss(); onShare() },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
        )
        if (onExportMarkdown != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_markdown)) },
                onClick = { onDismiss(); onExportMarkdown() },
                leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) }
            )
        }
        if (onExportText != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_text)) },
                onClick = { onDismiss(); onExportText() },
                leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_note)) },
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
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(note.id) { mutableStateOf(false) }
    var draftTag by rememberSaveable(note.id) { mutableStateOf("") }
    var bodyField by remember(note.id) {
        mutableStateOf(
            TextFieldValue(note.body, selection = TextRange(note.body.length))
        )
    }
    var closeWhenSaved by rememberSaveable(note.id) { mutableStateOf(false) }
    var exportInProgress by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val exportSuccess = stringResource(R.string.export_success)
    val transferError = stringResource(R.string.transfer_error)
    val markdownPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null && !exportInProgress) {
            exportScope.launch {
                exportInProgress = true
                val result = runCatching {
                    val current = viewModel.exportSnapshot().first { it.id == note.id }
                    NoteDocumentTransfer.writeNote(context, uri, current, markdown = true)
                }
                exportInProgress = false
                snackbar.showSnackbar(
                    result.fold({ exportSuccess }, { transferError + (it.localizedMessage ?: "") })
                )
            }
        }
    }
    val textPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && !exportInProgress) {
            exportScope.launch {
                exportInProgress = true
                val result = runCatching {
                    val current = viewModel.exportSnapshot().first { it.id == note.id }
                    NoteDocumentTransfer.writeNote(context, uri, current, markdown = false)
                }
                exportInProgress = false
                snackbar.showSnackbar(
                    result.fold({ exportSuccess }, { transferError + (it.localizedMessage ?: "") })
                )
            }
        }
    }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(note.id, note.body) {
        if (bodyField.text != note.body) {
            bodyField = bodyField.copy(text = note.body)
        }
    }

    fun formatBody(edit: MarkdownEdit) {
        bodyField = TextFieldValue(
            text = edit.text,
            selection = TextRange(edit.start, edit.end)
        )
        viewModel.changeBody(edit.text)
        bodyFocus.requestFocus()
    }

    fun requestClose() {
        when (saveState) {
            EditorSaveState.Saved -> viewModel.closeNote()
            EditorSaveState.Saving -> closeWhenSaved = true
            is EditorSaveState.Failed -> confirmDiscard = true
        }
    }

    BackHandler { requestClose() }
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
                }) { Text(stringResource(R.string.retry)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.closeNote()
                }) { Text(stringResource(R.string.leave_unsaved)) }
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
                    IconButton(onClick = { viewModel.togglePin(note) }) {
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
                        IconButton(onClick = { menuExpanded = true }) {
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
                            onExportMarkdown = {
                                markdownPicker.launch(safeExportName(note, "md"))
                            },
                            onExportText = {
                                textPicker.launch(safeExportName(note, "txt"))
                            }
                        )
                    }
                }
            )
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
                EditorSaveStatus(saveState = saveState, onRetry = viewModel::retrySave)
                Spacer(Modifier.height(24.dp))
                BasicTextField(
                    value = note.title,
                    onValueChange = viewModel::changeTitle,
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
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = draftTag,
                        onValueChange = { draftTag = it.take(64) },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.add_tag_hint)) },
                        modifier = Modifier.weight(1f).testTag("noma_tag_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.addTag(draftTag)
                            draftTag = ""
                        })
                    )
                    IconButton(
                        onClick = {
                            viewModel.addTag(draftTag)
                            draftTag = ""
                        },
                        enabled = draftTag.isNotBlank(),
                        modifier = Modifier.testTag("noma_tag_add")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_tag))
                    }
                }
                if (note.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        note.tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeTag(tag) },
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.remove_tag, tag),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        formatBody(MarkdownEdits.bold(
                            bodyField.text, bodyField.selection.start, bodyField.selection.end
                        ))
                    }, modifier = Modifier.testTag("noma_format_bold")) {
                        Text(stringResource(R.string.format_bold))
                    }
                    TextButton(onClick = {
                        formatBody(MarkdownEdits.heading(bodyField.text, bodyField.selection.start))
                    }) { Text(stringResource(R.string.format_heading)) }
                    TextButton(onClick = {
                        formatBody(MarkdownEdits.bullet(bodyField.text, bodyField.selection.start))
                    }) { Text(stringResource(R.string.format_bullet)) }
                    TextButton(onClick = {
                        formatBody(MarkdownEdits.checklist(bodyField.text, bodyField.selection.start))
                    }) { Text(stringResource(R.string.add_checklist_item)) }
                }
                val tasks = ChecklistSyntax.items(note.body)
                if (tasks.isNotEmpty()) {
                    Text(
                        stringResource(
                            R.string.checklist_progress,
                            tasks.count { it.checked },
                            tasks.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp)
                    ) {
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(role = Role.Checkbox) {
                                        viewModel.toggleChecklistItem(task.lineIndex)
                                    }
                                    .testTag("noma_checklist_${task.lineIndex}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.checked,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = task.text.ifEmpty { stringResource(R.string.untitled_task) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                BasicTextField(
                    value = bodyField,
                    onValueChange = { input ->
                        bodyField = input
                        viewModel.changeBody(input.text)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
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
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(NomaMotion.quick))
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
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance)) },
        text = {
            Column {
                val options = listOf(
                    NomaAppearance.SYSTEM to R.string.appearance_system,
                    NomaAppearance.LIGHT to R.string.appearance_light,
                    NomaAppearance.DARK to R.string.appearance_dark
                )
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { onSelect(mode) },
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
        }
    )
}

@Composable
private fun DeleteConfirmation(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_body)) },
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_note)))
}

/** Suggested names are advisory: the system document picker controls the actual file. */
private fun safeExportName(note: Note, extension: String): String {
    val base = note.displayTitle.take(48).map {
        if (it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ') it else '_'
    }.joinToString("").trim().ifBlank { "Noma-note" }
    return "$base.$extension"
}
