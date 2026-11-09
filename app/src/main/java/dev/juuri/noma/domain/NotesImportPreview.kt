package dev.juuri.noma.domain

/** A pure, non-mutating preview: no SQLite operations before user confirmation. */
object NotesImportPreview {
    fun duplicateCount(existing: List<Note>, incoming: List<Note>): Int {
        if (existing.isEmpty() || incoming.isEmpty()) return 0
        val ids = existing.mapTo(HashSet(existing.size)) { it.id }
        return incoming.count { ids.contains(it.id) }
    }
}
