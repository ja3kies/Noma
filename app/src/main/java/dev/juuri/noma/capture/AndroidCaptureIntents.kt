package dev.juuri.noma.capture

import android.content.Intent

sealed interface CaptureRequest {
    data object NewNote : CaptureRequest
    data class SharedText(val title: String, val body: String) : CaptureRequest
}

/** No file access, URI dereference, HTML execution, or implicit persistence. */
object AndroidCaptureIntents {
    const val ACTION_NEW_NOTE = "dev.juuri.noma.action.NEW_NOTE"
    const val MAX_BODY_LENGTH = 100_000
    const val MAX_TITLE_LENGTH = 200

    fun parse(intent: Intent?): CaptureRequest? {
        if (intent == null) return null
        if (intent.action == ACTION_NEW_NOTE) return CaptureRequest.NewNote
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return null

        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?.removePrefix("\uFEFF") ?: return null
        if (text.isBlank() || text.length > MAX_BODY_LENGTH) return null

        // A sender-controlled subject is treated as plaintext, never markup.
        val title = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
            ?.trim()?.take(MAX_TITLE_LENGTH).orEmpty()
        return CaptureRequest.SharedText(title, text)
    }
}
