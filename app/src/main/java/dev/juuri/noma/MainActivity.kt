package dev.juuri.noma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.juuri.noma.data.SqliteNotesRepository
import dev.juuri.noma.domain.NotesService
import dev.juuri.noma.ui.NomaApp
import dev.juuri.noma.ui.NotesViewModel
import dev.juuri.noma.ui.theme.NomaTheme

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
            NomaTheme {
                NomaApp(notesViewModel)
            }
        }
    }
}
