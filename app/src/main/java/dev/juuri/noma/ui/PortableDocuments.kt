package dev.juuri.noma.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
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
import dev.juuri.noma.data.NotesPlainTextFormat
import android.provider.OpenableColumns
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PortableDocuments(
    snapshot: NotesSnapshot,
    onImport: (List<Note>) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notReady = stringResource(R.string.portable_not_ready)
    val writeFailed = stringResource(R.string.portable_write_failed)
    val readFailed = stringResource(R.string.portable_read_failed)
    val exportResult = stringResource(R.string.portable_export_result)
    val importFailure = stringResource(R.string.portable_import_failed)
    val exportFailure = stringResource(R.string.portable_export_failed)
    var pending by remember { mutableStateOf<List<Note>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var importMenu by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            val notes = (snapshot as? NotesSnapshot.Ready)?.entries
            if (notes == null) onMessage(notReady)
            else {
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri, "wt")?.use {
                                it.write(NotesPortableFormat.encode(notes))
                                it.flush()
                            } ?: error(writeFailed)
                        }
                        onMessage(exportResult.format(notes.size))
                    } catch (e: Exception) {
                        onMessage(exportFailure.format(e.localizedMessage ?: writeFailed))
                    } finally { busy = false }
                }
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
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val size = stream.read(buffer)
                                if (size < 0) break
                                output.write(buffer, 0, size)
                                require(output.size() <= NotesPortableFormat.MAX_IMPORT_BYTES) {
                                    "File exceeds size limit"
                                }
                            }
                            output.toByteArray()
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
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                try {
                    pending = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use {
                            val out = java.io.ByteArrayOutputStream()
                            val chunk = ByteArray(8192)
                            while (true) {
                                val count = it.read(chunk)
                                if (count < 0) break
                                out.write(chunk, 0, count)
                                require(out.size() <= NotesPortableFormat.MAX_IMPORT_BYTES) {
                                    "Archive exceeds size limit"
                                }
                            }
                            out.toByteArray()
                        } ?: error(readFailed)
                        NotesPortableFormat.decode(bytes)
                    }
                } catch (e: Exception) {
                    onMessage(importFailure.format(e.localizedMessage ?: readFailed))
                } finally { busy = false }
            }
        }
    }
    Row {
        Box {
            TextButton(
                onClick = { importMenu = true },
                enabled = !busy && snapshot is NotesSnapshot.Ready
            ) { Text(stringResource(R.string.portable_import_action)) }
            DropdownMenu(expanded = importMenu, onDismissRequest = { importMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.portable_import_json)) },
                    onClick = {
                        importMenu = false
                        importer.launch(arrayOf("application/json"))
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.portable_import_text)) },
                    onClick = {
                        importMenu = false
                        textImporter.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown"))
                    }
                )
            }
        }
        TextButton(
            onClick = { exporter.launch("noma-notes.json") },
            enabled = !busy && snapshot is NotesSnapshot.Ready
        ) { Text(stringResource(R.string.portable_export_json)) }
    }
    pending?.let { notes ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.portable_import_confirm, notes.size)) },
            text = { Text(stringResource(R.string.portable_import_warning, notes.count { note -> (snapshot as? NotesSnapshot.Ready)?.entries?.any { it.id == note.id } == true })) },
            confirmButton = {
                TextButton(onClick = { onImport(notes); pending = null }) { Text(stringResource(R.string.portable_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
