package dev.juuri.noma.domain

/** Inbound share payloads contain user-supplied data and must remain bounded. */
data class NoteCapture(val title: String, val body: String)

object NoteCaptureParser {
    const val MAX_INCOMING_BODY = 100_000
    const val MAX_INCOMING_TITLE = 256

    fun parse(title: CharSequence?, text: CharSequence?): NoteCapture? {
        val raw = text?.toString() ?: return null
        if (raw.isBlank()) return null
        val body = raw.take(MAX_INCOMING_BODY)
        val safeTitle = title?.toString()?.trim()
            ?.replace('\r', ' ')?.replace('\n', ' ')
            ?.take(MAX_INCOMING_TITLE)
            .orEmpty()
        return NoteCapture(safeTitle, body)
    }
}
