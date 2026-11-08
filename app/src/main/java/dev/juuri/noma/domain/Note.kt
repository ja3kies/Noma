package dev.juuri.noma.domain

data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList()
) {
    val displayTitle: String
        get() = title.trim().ifEmpty { body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }

    val preview: String
        get() = body.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty()
}
