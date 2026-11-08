package dev.juuri.noma.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.juuri.noma.domain.Note

/**
 * Private SQLite storage with no external permissions or network access.
 * Schema version changes must ship a migration; never drop the table on upgrade.
 */
internal class NotesOpenHelper(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE notes (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                pinned INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0,
                tags TEXT NOT NULL DEFAULT '[]',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC)")
        db.execSQL("CREATE INDEX idx_notes_pinned ON notes(pinned DESC, updated_at DESC)")
        db.execSQL("CREATE INDEX idx_notes_archived ON notes(archived, updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion
        if (version == 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX idx_notes_archived ON notes(archived, updated_at DESC)")
            version = 2
        }
        if (version == 2 && newVersion >= 3) {
            db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
            version = 3
        }
        check(version == newVersion) { "Missing migration from $oldVersion to $newVersion" }
    }

    fun readAll(): List<Note> = buildList {
        readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "pinned", "created_at", "updated_at", "archived", "tags"),
            null, null, null, null, "pinned DESC, updated_at DESC, id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                add(
                    Note(
                        id = cursor.getString(0),
                        title = cursor.getString(1),
                        body = cursor.getString(2),
                        isPinned = cursor.getInt(3) != 0,
                        createdAt = cursor.getLong(4),
                        updatedAt = cursor.getLong(5),
                        isArchived = cursor.getInt(6) != 0,
                        tags = decodeTags(cursor.getString(7))
                    )
                )
            }
        }
    }

    fun upsert(note: Note) {
        val values = ContentValues().apply {
            put("id", note.id)
            put("title", note.title)
            put("body", note.body)
            put("pinned", if (note.isPinned) 1 else 0)
            put("archived", if (note.isArchived) 1 else 0)
            put("tags", org.json.JSONArray(note.tags).toString())
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
        }
        check(
            writableDatabase.insertWithOnConflict(
                "notes", null, values, SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        ) { "Could not save note" }
    }

    /**
     * All-or-nothing restore: one failed row must not leave a partial backup.
     * Callers resolve existing IDs before the transaction.
     */
    fun insertBatch(entries: List<Note>) {
        val database = writableDatabase
        database.beginTransaction()
        try {
            for (note in entries) {
                val values = ContentValues().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("pinned", if (note.isPinned) 1 else 0)
                    put("archived", if (note.isArchived) 1 else 0)
                    put("tags", org.json.JSONArray(note.tags).toString())
                    put("created_at", note.createdAt)
                    put("updated_at", note.updatedAt)
                }
                check(database.insertWithOnConflict(
                    "notes", null, values, SQLiteDatabase.CONFLICT_ABORT
                ) != -1L) { "Import failed" }
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun remove(id: String) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id))
    }

    private fun decodeTags(value: String): List<String> {
        val entries = org.json.JSONArray(value)
        return (0 until entries.length()).map(entries::getString)
    }

    companion object {
        internal const val DATABASE_NAME = "noma-notes.db"
        private const val DATABASE_VERSION = 3
    }
}
