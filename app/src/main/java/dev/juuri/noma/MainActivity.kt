package dev.juuri.noma

import android.os.Bundle
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import dev.juuri.noma.capture.AndroidCaptureIntents
import dev.juuri.noma.capture.CaptureRequest
import dev.juuri.noma.capture.NomaWidgetPreferences
import dev.juuri.noma.capture.NomaQuickCaptureWidget
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.juuri.noma.data.SqliteNotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.ui.NomaApp
import dev.juuri.noma.ui.NotesViewModel
import dev.juuri.noma.ui.theme.NomaAppearance
import dev.juuri.noma.ui.theme.NomaTheme
import dev.juuri.noma.ui.theme.isNomaDark

class MainActivity : ComponentActivity() {
    private val notesViewModel by viewModels<NotesViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NotesViewModel::class.java))
                val repository = SqliteNotesRepository(applicationContext)
                return NotesViewModel(repository, NotesService(repository)) as T
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptCaptureIntent(intent)
    }

    private fun acceptCaptureIntent(incoming: Intent?) {
        val request = try {
            AndroidCaptureIntents.parse(incoming)
        } catch (_: RuntimeException) {
            // Malformed or hostile Intent must not crash a note-taking app.
            null
        }
        when (request) {
            is CaptureRequest.NewNote -> notesViewModel.requestQuickNote()
            is CaptureRequest.SharedText -> notesViewModel.receiveSharedText(request)
            null -> Unit
        }
        // Avoid presenting the same share a second time after Activity recreation.
        setIntent(Intent(Intent.ACTION_MAIN))
    }

    private fun registerQuickCaptureShortcut() {
        val shortcuts = getSystemService(ShortcutManager::class.java) ?: return
        val target = Intent(this, MainActivity::class.java).apply {
            action = AndroidCaptureIntents.ACTION_NEW_NOTE
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            // Some OEM launchers deny the entire shortcut API, including construction.
            // Quick capture is optional and must not prevent opening the notebook.
            val entry = ShortcutInfo.Builder(this, "noma-new-note")
                .setShortLabel(getString(R.string.shortcut_new_note))
                .setLongLabel(getString(R.string.shortcut_new_note_detail))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_noma_launcher))
                .setIntent(target)
                .build()
            shortcuts.addDynamicShortcuts(listOf(entry))
        } catch (failure: RuntimeException) {
            android.util.Log.w("NomaCapture", "Launcher shortcut unavailable", failure)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Unconfirmed text stays editable after a process recreation.
        notesViewModel.pendingShare.value?.let { pending ->
            outState.putString("noma_pending_share_title", pending.title)
            outState.putString("noma_pending_share_body", pending.body)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (notesViewModel.pendingShare.value == null) {
            val body = savedInstanceState?.getString("noma_pending_share_body")
            if (!body.isNullOrBlank()) {
                notesViewModel.receiveSharedText(CaptureRequest.SharedText(
                    savedInstanceState.getString("noma_pending_share_title").orEmpty(),
                    body
                ))
            }
        }
        registerQuickCaptureShortcut()
        acceptCaptureIntent(intent)
        setContent {
            val preferences = remember {
                getSharedPreferences("noma_appearance", MODE_PRIVATE)
            }
            var appearance by remember {
                mutableStateOf(NomaAppearance.fromStorage(preferences.getString("mode", null)))
            }
            var widgetStyle by remember {
                mutableStateOf(NomaWidgetPreferences.read(applicationContext))
            }
            val dark = isNomaDark(appearance)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            NomaTheme(appearance) {
                NomaApp(
                    viewModel = notesViewModel,
                    appearance = appearance,
                    widgetStyle = widgetStyle,
                    onWidgetStyleChange = { selected ->
                        try {
                            // Write first: never show a selection that cannot survive restart.
                            NomaWidgetPreferences.save(applicationContext, selected)
                            widgetStyle = selected
                        } catch (failure: RuntimeException) {
                            android.util.Log.e("NomaWidget", "Cannot save widget style", failure)
                            android.widget.Toast.makeText(
                                this, R.string.widget_style_save_error,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        try {
                            NomaQuickCaptureWidget.refreshAll(applicationContext)
                        } catch (failure: RuntimeException) {
                            android.util.Log.w("NomaWidget", "Widget host refused refresh", failure)
                        }
                    },
                    onAppearanceChange = { selected ->
                        appearance = selected
                        preferences.edit().putString("mode", selected.name).apply()
                    }
                )
            }
        }
    }
}
