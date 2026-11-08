package dev.juuri.noma

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dev.juuri.noma.domain.NoteCaptureParser

/**
 * No UI and no network request: receive explicit Android text shares, then
 * forward sanitized content to the active Noma instance. Never read arbitrary URIs.
 */
class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent
        if (incoming?.type == "text/plain" &&
            (incoming.action == Intent.ACTION_SEND || incoming.action == Intent.ACTION_PROCESS_TEXT)
        ) {
            val selected = if (incoming.action == Intent.ACTION_PROCESS_TEXT)
                incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)
            val payload = NoteCaptureParser.parse(
                incoming.getCharSequenceExtra(Intent.EXTRA_SUBJECT),
                selected
            )
            if (payload != null) {
                val destination = Intent(this, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_CAPTURE_SHARED_TEXT
                    putExtra(MainActivity.EXTRA_CAPTURE_TITLE, payload.title)
                    putExtra(MainActivity.EXTRA_CAPTURE_BODY, payload.body)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(destination)
            }
        }
        finish()
    }
}
