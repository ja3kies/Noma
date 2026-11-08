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
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val payload = NoteCaptureParser.parse(
                intent?.getCharSequenceExtra(Intent.EXTRA_SUBJECT),
                intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)
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
