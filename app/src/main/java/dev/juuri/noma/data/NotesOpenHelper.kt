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
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC)")
        db.execSQL("CREATE INDEX idx_notes_pinned ON notes(pinned DESC, updated_at DESC)")
        db.execSQL("CREATE INDEX idx_notes_archived ON notes(archived, updated_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX idx_notes_archived ON notes(archived, updated_at DESC)")
        } else {
            throw IllegalStateException("Missing migration from $oldVersion to $newVersion")
        }
    }

    fun readAll(): List<Note> = buildList {
        readableDatabase.query(
            "notes",
            arrayOf("id", "title", "body", "pinned", "created_at", "updated_at", "archived"),
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
                        isArchived = cursor.getInt(6) != 0
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
            put("created_at", note.createdAt)
            put("updated_at", note.updatedAt)
        }
        check(
            writableDatabase.insertWithOnConflict(
                "notes", null, values, SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        ) { "Could not save note" }
    }

    /** One SQLite transaction: either all eligible imported notes persist or none do. */
    fun importMissing(notes: List<Note>): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var inserted = 0
            for (note in notes) {
                val values = ContentValues().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("pinned", if (note.isPinned) 1 else 0)
                    put("archived", if (note.isArchived) 1 else 0)
                    put("created_at", note.createdAt)
                    put("updated_at", note.updatedAt)
                }
                if (db.insertWithOnConflict("notes", null, values,
                        SQLiteDatabase.CONFLICT_IGNORE) != -1L) {
                    inserted++
                }
            }
            db.setTransactionSuccessful()
            return inserted
        } finally {
            db.endTransaction()
        }
    }

    fun remove(id: String) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id))
    }

    companion object {
        internal const val DATABASE_NAME = "noma-notes.db"
        private const val DATABASE_VERSION = 2
    }
}
