package dev.juuri.noma.capture

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCaptureIntentsTest {
    @Test fun recognizesExplicitQuickCaptureShortcut() {
        val request = AndroidCaptureIntents.parse(
            Intent(AndroidCaptureIntents.ACTION_NEW_NOTE)
        )
        assertEquals(CaptureRequest.NewNote, request)
    }

    @Test fun acceptsOnlyPlainTextShares() {
        val shared = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.org/article")
            putExtra(Intent.EXTRA_SUBJECT, "Article title")
        }
        assertEquals(
            CaptureRequest.SharedText("Article title", "https://example.org/article"),
            AndroidCaptureIntents.parse(shared)
        )
        assertNull(AndroidCaptureIntents.parse(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, "Never treat this as text")
        }))
    }

    @Test fun doesNotTreatOrdinaryLaunchAsNewNote() {
        assertNull(AndroidCaptureIntents.parse(Intent(Intent.ACTION_MAIN)))
        assertNull(AndroidCaptureIntents.parse(null))
    }

    @Test fun stripsBomAndRejectsBlankText() {
        val clean = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "\uFEFFBonjour Noma")
        }
        assertEquals("Bonjour Noma",
            (AndroidCaptureIntents.parse(clean) as CaptureRequest.SharedText).body)
        val blank = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, " \t\n")
        }
        assertNull(AndroidCaptureIntents.parse(blank))
    }

    @Test fun rejectsOversizeAndMissingSharedContent() {
        val tooLarge = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "x".repeat(AndroidCaptureIntents.MAX_BODY_LENGTH + 1))
        }
        assertNull(AndroidCaptureIntents.parse(tooLarge))
        assertNull(AndroidCaptureIntents.parse(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
        }))
    }

    @Test fun truncatesSenderControlledSubjectButNotBody() {
        val incoming = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Important notes")
            putExtra(Intent.EXTRA_SUBJECT, "a".repeat(400))
        }
        val request = AndroidCaptureIntents.parse(incoming) as CaptureRequest.SharedText
        assertEquals(200, request.title.length)
        assertEquals("Important notes", request.body)
        assertTrue(request.title.all { it == 'a' })
    }

    @Test fun ignoresUnsupportedIntentAndHtml() {
        assertNull(AndroidCaptureIntents.parse(Intent(Intent.ACTION_VIEW)))
        assertNull(AndroidCaptureIntents.parse(Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_TEXT, "<b>unsafe</b>")
        }))
    }
}
