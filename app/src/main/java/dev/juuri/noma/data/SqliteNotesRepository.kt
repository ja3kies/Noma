package dev.juuri.noma.data

import android.content.Context
import dev.juuri.noma.domain.Note
import dev.juuri.noma.domain.NotesRepository
import dev.juuri.noma.domain.NotesSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SqliteNotesRepository(
    context: Context,
    databaseName: String = NotesOpenHelper.DATABASE_NAME
) : NotesRepository {
    private val helper = NotesOpenHelper(context.applicationContext, databaseName)
    private val mutex = Mutex()
    private val io = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val state = MutableStateFlow<NotesSnapshot>(NotesSnapshot.Loading)

    override val notes: StateFlow<NotesSnapshot> = state

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() = withContext(io) {
        mutex.withLock {
            try {
                state.value = NotesSnapshot.Ready(helper.readAll())
            } catch (failure: Exception) {
                state.value = NotesSnapshot.Error(failure.localizedMessage ?: "Cannot read notes")
            }
        }
    }

    override suspend fun save(note: Note) = withContext(io) {
        mutex.withLock {
            helper.upsert(note)
            state.value = NotesSnapshot.Ready(helper.readAll())
        }
    }

    override suspend fun importMissing(notes: List<Note>): Int = withContext(io) {
        mutex.withLock {
            val inserted = helper.importMissing(notes)
            state.value = NotesSnapshot.Ready(helper.readAll())
            inserted
        }
    }

    override suspend fun delete(id: String) = withContext(io) {
        mutex.withLock {
            helper.remove(id)
            state.value = NotesSnapshot.Ready(helper.readAll())
        }
    }
}
