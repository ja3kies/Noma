package dev.juuri.noma.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.juuri.noma.R
import dev.juuri.noma.data.NotesPortableFormat
import dev.juuri.noma.data.NotesBackupVerification
import dev.juuri.noma.data.NotesArchiveSummary
import dev.juuri.noma.data.NotesDocumentIo
import dev.juuri.noma.data.NotesPlainTextFormat
import android.provider.OpenableColumns
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesImportPreview
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PortableDocuments(
    snapshot: NotesSnapshot,
    onImport: (List<Note>) -> Unit,
    snapshotForExport: suspend () -> List<Note>,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notReady = stringResource(R.string.portable_not_ready)
    val writeFailed = stringResource(R.string.portable_write_failed)
    val readFailed = stringResource(R.string.portable_read_failed)
    val exportResult = stringResource(R.string.portable_export_result)
    val unverifiedExport = stringResource(R.string.portable_export_unverified)
    val importFailure = stringResource(R.string.portable_import_failed)
    val verificationFailure = stringResource(R.string.portable_verify_failed)
    val exportFailure = stringResource(R.string.portable_export_failed)
    var pending by remember { mutableStateOf<List<Note>?>(null) }
    var inspectedArchive by remember { mutableStateOf<NotesArchiveSummary?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionsExpanded by remember { mutableStateOf(false) }
    var confirmPlaintextExport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val (count, verification) = withContext(Dispatchers.IO) {
                        val notes = snapshotForExport()
                        val bytes = NotesPortableFormat.encode(notes)
                        val status = NotesBackupVerification.writeAndVerify(
                            bytes,
                            openWrite = { context.contentResolver.openOutputStream(uri, "wt") },
                            openRead = { context.contentResolver.openInputStream(uri) }
                        )
                        notes.size to status
                    }
                    onMessage(
                        if (verification == NotesBackupVerification.Status.VERIFIED)
                            exportResult.format(count)
                        else unverifiedExport
                    )
                } catch (e: Exception) {
                    onMessage(exportFailure.format(e.localizedMessage ?: writeFailed))
                } finally { busy = false }
            }
        }
    }
    val textImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    val note = withContext(Dispatchers.IO) {
                        val filename = context.contentResolver.query(
                            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                            NotesDocumentIo.readBounded(stream)
                        } ?: error(readFailed)
                        NotesPlainTextFormat.decode(bytes, filename)
                    }
                    pending = listOf(note)
                } catch (e: Exception) {
                    onMessage(importFailure.format(e.localizedMessage ?: readFailed))
                } finally { busy = false }
            }
        }
    }
    val verifier = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            inspectedArchive = null
            busy = true
            scope.launch {
                try {
                    inspectedArchive = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                            NotesDocumentIo.readBounded(stream)
                        } ?: error(readFailed)
                        NotesArchiveSummary.inspect(bytes)
                    }
                } catch (failure: Exception) {
                    onMessage(verificationFailure.format(failure.localizedMessage ?: readFailed))
                } finally {
                    busy = false
                }
            }
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    pending = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use {
                            NotesDocumentIo.readBounded(it)
                        } ?: error(readFailed)
                        NotesPortableFormat.decode(bytes)
                    }
                } catch (e: Exception) {
                    onMessage(importFailure.format(e.localizedMessage ?: readFailed))
                } finally { busy = false }
            }
        }
    }
    Box {
        IconButton(
            onClick = { actionsExpanded = true },
            enabled = !busy
        ) {
            Icon(
                Icons.Outlined.ImportExport,
                contentDescription = stringResource(R.string.portable_actions)
            )
        }
        DropdownMenu(
            expanded = actionsExpanded,
            onDismissRequest = { actionsExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_verify_action)) },
                enabled = !busy,
                onClick = {
                    actionsExpanded = false
                    verifier.launch(arrayOf("application/json"))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_import_json)) },
                enabled = snapshot is NotesSnapshot.Ready && !busy,
                onClick = {
                    actionsExpanded = false
                    importer.launch(arrayOf("application/json"))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_import_text)) },
                enabled = snapshot is NotesSnapshot.Ready && !busy,
                onClick = {
                    actionsExpanded = false
                    textImporter.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown"))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_export_json)) },
                enabled = snapshot is NotesSnapshot.Ready && !busy,
                onClick = {
                    actionsExpanded = false
                    confirmPlaintextExport = true
                }
            )
        }
    }
    inspectedArchive?.let { summary ->
        AlertDialog(
            onDismissRequest = { inspectedArchive = null },
            title = { Text(stringResource(R.string.portable_verify_success_title)) },
            text = {
                Text(stringResource(
                    R.string.portable_verify_summary,
                    summary.total,
                    summary.pinned,
                    summary.archived
                ) + "\n" + stringResource(R.string.portable_verify_no_import))
            },
            confirmButton = {
                TextButton(onClick = { inspectedArchive = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
    if (confirmPlaintextExport) {
        AlertDialog(
            onDismissRequest = { confirmPlaintextExport = false },
            title = { Text(stringResource(R.string.portable_export_warning_title)) },
            text = { Text(stringResource(R.string.portable_export_warning_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmPlaintextExport = false
                    exporter.launch("noma-notes.json")
                }) { Text(stringResource(R.string.portable_export_json)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPlaintextExport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    pending?.let { notes ->
        val duplicateCount = remember(notes, snapshot) {
            val existing = (snapshot as? NotesSnapshot.Ready)?.entries.orEmpty()
            NotesImportPreview.duplicateCount(existing, notes)
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.portable_import_confirm, notes.size)) },
            text = { Text(stringResource(R.string.portable_import_warning, duplicateCount)) },
            confirmButton = {
                TextButton(
                    enabled = snapshot is NotesSnapshot.Ready && !busy,
                    onClick = { onImport(notes); pending = null }
                ) { Text(stringResource(R.string.portable_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
