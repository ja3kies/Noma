package dev.juuri.noma.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.juuri.noma.R
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NoteSort
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.flow.collect
import java.text.DateFormat
import java.util.Date

@Composable
fun NomaApp(viewModel: NotesViewModel) {
    val snapshot by viewModel.snapshot.collectAsState()
    val query by viewModel.query.collectAsState()
    val pinnedOnly by viewModel.pinnedOnly.collectAsState()
    val archivedOnly by viewModel.archivedOnly.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val active by viewModel.active.collectAsState()
    val creating by viewModel.creating.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.deleted_message)
    val archivedMessage = stringResource(R.string.archived_message)
    val unarchivedMessage = stringResource(R.string.unarchived_message)
    val undoLabel = stringResource(R.string.undo)
    val errorPrefix = stringResource(R.string.error_prefix)
    LaunchedEffect(viewModel, deletedMessage, archivedMessage, unarchivedMessage, undoLabel, errorPrefix) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteUiEvent.Deleted -> {
                    val outcome = snackbar.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        withDismissAction = true
                    )
                    if (outcome == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.note)
                    }
                }
                is NoteUiEvent.ArchiveChanged -> {
                    val outcome = snackbar.showSnackbar(
                        message = if (event.previous.isArchived) unarchivedMessage else archivedMessage,
                        actionLabel = undoLabel,
                        withDismissAction = true
                    )
                    if (outcome == SnackbarResult.ActionPerformed) {
                        viewModel.undoArchive(event.previous)
                    }
                }
                is NoteUiEvent.Failure ->
                    snackbar.showSnackbar(errorPrefix + event.message, withDismissAction = true)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (active == null) {
            NotesListScreen(
                snapshot = snapshot,
                query = query,
                pinnedOnly = pinnedOnly,
                archivedOnly = archivedOnly,
                sort = sort,
                creating = creating,
                filter = viewModel::visible,
                onQueryChange = viewModel::onQueryChange,
                onTogglePinnedOnly = viewModel::togglePinnedOnly,
                onToggleArchiveView = viewModel::toggleArchiveView,
                onSortChange = viewModel::selectSort,
                onCreate = viewModel::createNote,
                onOpen = viewModel::openNote,
                onTogglePin = viewModel::togglePin,
                onToggleArchive = viewModel::toggleArchive,
                onDelete = viewModel::deleteNote,
                onRetry = viewModel::retry,
                snackbar = snackbar
            )
        } else {
            NoteEditorScreen(
                note = requireNotNull(active),
                onBack = viewModel::closeNote,
                onTitleChange = viewModel::changeTitle,
                onBodyChange = viewModel::changeBody,
                onTogglePin = { viewModel.togglePin(requireNotNull(active)) },
                onToggleArchive = { viewModel.toggleArchive(requireNotNull(active)) },
                onDelete = { viewModel.deleteNote(requireNotNull(active)) },
                snackbar = snackbar
            )
        }
    }
}

@Composable
private fun NotesListScreen(
    snapshot: NotesSnapshot,
    query: String,
    pinnedOnly: Boolean,
    archivedOnly: Boolean,
    sort: NoteSort,
    creating: Boolean,
    filter: (List<Note>, String, Boolean, Boolean, NoteSort) -> List<Note>,
    onQueryChange: (String) -> Unit,
    onTogglePinnedOnly: () -> Unit,
    onToggleArchiveView: () -> Unit,
    onSortChange: (NoteSort) -> Unit,
    onCreate: () -> Unit,
    onOpen: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleArchive: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onRetry: () -> Unit,
    snackbar: SnackbarHostState
) {
    val newNoteLabel = stringResource(R.string.new_note)
    var deleting by remember { mutableStateOf<Note?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    if (deleting != null) {
        DeleteConfirmation(
            onCancel = { deleting = null },
            onConfirm = {
                deleting?.let(onDelete)
                deleting = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                expanded = true,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_note)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                modifier = Modifier.semantics {
                    contentDescription = newNoteLabel
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 18.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(25.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.clear_search))
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = archivedOnly,
                        onClick = onToggleArchiveView,
                        label = { Text(stringResource(R.string.archive_view)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    if (!archivedOnly) {
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = pinnedOnly,
                            onClick = onTogglePinnedOnly,
                            label = { Text(stringResource(R.string.pinned_filter)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = stringResource(R.string.sort_notes))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_recent)) },
                                onClick = { onSortChange(NoteSort.RECENT); sortMenuExpanded = false },
                                trailingIcon = if (sort == NoteSort.RECENT) {
                                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
                                } else null
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_title)) },
                                onClick = { onSortChange(NoteSort.TITLE); sortMenuExpanded = false },
                                trailingIcon = if (sort == NoteSort.TITLE) {
                                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
                if (snapshot is NotesSnapshot.Ready) {
                    Text(
                        text = stringResource(
                            R.string.note_total,
                            snapshot.entries.count { it.isArchived == archivedOnly }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (snapshot) {
                NotesSnapshot.Loading -> CenterMessage {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.loading_notes))
                }
                is NotesSnapshot.Error -> CenterMessage {
                    Icon(
                        Icons.Outlined.Refresh, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.unavailable))
                    Spacer(Modifier.height(8.dp))
                    Text(snapshot.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                is NotesSnapshot.Ready -> {
                    val visible = remember(snapshot.entries, query, pinnedOnly, archivedOnly, sort) {
                        filter(snapshot.entries, query, pinnedOnly, archivedOnly, sort)
                    }
                    if (visible.isEmpty()) {
                        CenterMessage {
                            Icon(
                                if (query.isNotBlank() || pinnedOnly) Icons.Outlined.Search
                                else if (archivedOnly) Icons.Outlined.Archive
                                else Icons.Outlined.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                stringResource(
                                    if (query.isNotBlank() || pinnedOnly) R.string.no_results
                                    else if (archivedOnly) R.string.empty_archive
                                    else R.string.empty_title
                                ),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                stringResource(
                                    if (query.isNotBlank() || pinnedOnly) R.string.no_results_detail
                                    else if (archivedOnly) R.string.empty_archive_detail
                                    else R.string.empty_detail
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val pinned = if (archivedOnly) emptyList() else visible.filter { it.isPinned }
                        val others = if (archivedOnly) visible else visible.filterNot { it.isPinned }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 105.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pinned.isNotEmpty()) {
                                item(key = "pinnedHeader") {
                                    SectionHeader(stringResource(R.string.pinned_section))
                                }
                                items(pinned, key = { it.id }) { note ->
                                    NoteCard(note, onOpen, onTogglePin, onToggleArchive, { deleting = it })
                                }
                            }
                            if (others.isNotEmpty()) {
                                item(key = "recentHeader") {
                                    SectionHeader(stringResource(if (archivedOnly) R.string.archived_section else R.string.recent_section))
                                }
                                items(others, key = { it.id }) { note ->
                                    NoteCard(note, onOpen, onTogglePin, onToggleArchive, { deleting = it })
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
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 3.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NoteCard(
    note: Note,
    onOpen: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleArchive: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { onOpen(note) })
                .padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.displayTitle.ifBlank { stringResource(R.string.untitled) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (note.preview.isNotEmpty() && note.title.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = note.preview,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = formatEdited(note.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (note.isPinned) {
                Icon(
                    Icons.Outlined.PushPin, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 5.dp).size(18.dp)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = stringResource(R.string.note_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(stringResource(if (note.isPinned) R.string.unpin_note else R.string.pin_note))
                        },
                        onClick = {
                            showMenu = false
                            onTogglePin(note)
                        },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(if (note.isArchived) R.string.unarchive_note else R.string.archive_note)) },
                        onClick = {
                            showMenu = false
                            onToggleArchive(note)
                        },
                        leadingIcon = {
                            Icon(
                                if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                contentDescription = null
                            )
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_note)) },
                        onClick = {
                            showMenu = false
                            onDelete(note)
                        },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note: Note,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    snackbar: SnackbarHostState
) {
    var confirmDelete by rememberSaveable(note.id) { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    if (confirmDelete) {
        DeleteConfirmation(
            onCancel = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            }
        )
    }

    val focusManager = LocalFocusManager.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.editor_nav_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_to_notes))
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (note.isPinned) R.string.unpin_note else R.string.pin_note
                            ),
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleArchive) {
                        Icon(
                            if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            contentDescription = stringResource(
                                if (note.isArchived) R.string.unarchive_note else R.string.archive_note
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            stringResource(R.string.delete_note),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.autosave_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatEdited(note.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = note.title,
                onValueChange = onTitleChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (note.title.isEmpty()) {
                            Text(
                                stringResource(R.string.title_hint),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = note.body,
                onValueChange = onBodyChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(500.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (note.body.isEmpty()) {
                            Text(
                                stringResource(R.string.body_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
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
private fun formatEdited(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestamp))
