package dev.juuri.noma.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import dev.juuri.noma.R
import dev.juuri.noma.data.NotesPortableFormat
import dev.juuri.noma.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Export a snapshot of the current note to the user-selected document destination. */
@Composable
internal fun PortableNoteExport(note: Note, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(note) }
    val success = stringResource(R.string.portable_note_export_done)
    val failure = stringResource(R.string.portable_export_failed)
    val writeFailed = stringResource(R.string.portable_write_failed)

    fun write(uri: Uri?, markdown: Boolean) {
        if (uri == null || busy) return
        busy = true
        val exporting = selected
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val contents = if (markdown) NotesPortableFormat.toMarkdown(exporting)
                        else NotesPortableFormat.toText(exporting)
                    context.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(contents.toByteArray(Charsets.UTF_8))
                        it.flush()
                    } ?: error(writeFailed)
                }
                onMessage(success)
            } catch (e: Exception) {
                onMessage(failure.format(e.localizedMessage ?: writeFailed))
            } finally { busy = false }
        }
    }

    val txt = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> write(uri, false) }
    val markdown = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> write(uri, true) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = !busy) {
            Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.portable_export_note))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_export_txt)) },
                onClick = {
                    selected = note
                    expanded = false
                    txt.launch(NotesPortableFormat.suggestedName(note, "txt"))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.portable_export_md)) },
                onClick = {
                    selected = note
                    expanded = false
                    markdown.launch(NotesPortableFormat.suggestedName(note, "md"))
                }
            )
        }
    }
}
