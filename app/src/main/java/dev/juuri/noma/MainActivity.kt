package dev.juuri.noma

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
    }
}
