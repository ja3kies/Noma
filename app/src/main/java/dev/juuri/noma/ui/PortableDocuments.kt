package dev.juuri.noma.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
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
        TextButton(onClick = { importer.launch(arrayOf("application/json")) }, enabled = !busy) {
            Text(stringResource(R.string.portable_import_json))
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
