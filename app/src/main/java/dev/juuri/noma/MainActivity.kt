package dev.juuri.noma

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerCaptureShortcut()
        setContent {
            val preferences = remember {
                getSharedPreferences("noma_appearance", MODE_PRIVATE)
            }
            var appearance by remember {
                mutableStateOf(NomaAppearance.fromStorage(preferences.getString("mode", null)))
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
                    onAppearanceChange = { selected ->
                        appearance = selected
                        preferences.edit().putString("mode", selected.name).apply()
                    }
                )
            }
        }
        handleCaptureIntent(intent)
    }

    /** Launcher long-press shortcut uses the actual package ID (including debug suffix). */
    private fun registerCaptureShortcut() {
        val manager = getSystemService(ShortcutManager::class.java) ?: return
        runCatching {
            val captureIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_CREATE_FROM_WIDGET
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            manager.dynamicShortcuts = listOf(
                ShortcutInfo.Builder(this, "noma_quick_capture")
                    .setShortLabel(getString(R.string.widget_new_note))
                    .setLongLabel(getString(R.string.shortcut_new_note))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_noma_launcher))
                    .setIntent(captureIntent)
                    .build()
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCaptureIntent(intent)
    }

    private fun handleCaptureIntent(incoming: Intent?) {
        when (incoming?.action) {
            ACTION_CREATE_FROM_WIDGET -> notesViewModel.createNote()
            ACTION_CAPTURE_SHARED_TEXT -> {
                val title = incoming.getStringExtra(EXTRA_CAPTURE_TITLE).orEmpty()
                val body = incoming.getStringExtra(EXTRA_CAPTURE_BODY).orEmpty()
                notesViewModel.captureSharedText(title, body)
            }
        }
        // A rotation or process recreation must not replay the same share.
        if (incoming?.action == ACTION_CREATE_FROM_WIDGET ||
            incoming?.action == ACTION_CAPTURE_SHARED_TEXT) {
            setIntent(Intent(this, MainActivity::class.java).apply { action = Intent.ACTION_MAIN })
        }
    }

    companion object {
        const val ACTION_CREATE_FROM_WIDGET = "dev.juuri.noma.action.NEW_NOTE"
        const val ACTION_CAPTURE_SHARED_TEXT = "dev.juuri.noma.action.SHARED_TEXT"
        const val EXTRA_CAPTURE_TITLE = "noma_capture_title"
        const val EXTRA_CAPTURE_BODY = "noma_capture_body"
    }
}
