package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

class NoteCaptureParserTest {
    @Test fun ignoresNullPayload() {
        assertNull(NoteCaptureParser.parse("subject", null))
    }

    @Test fun ignoresEmptyPayload() {
        assertNull(NoteCaptureParser.parse(null, "  \n  "))
    }

    @Test fun preservesOriginalBodyWithoutTrimming() {
        assertEquals("  list \nnext  ", NoteCaptureParser.parse(null, "  list \nnext  ")?.body)
    }

    @Test fun restrictsTitleAndRemovesLineBreaks() {
        val title = "A\nB\r" + "a".repeat(400)
        val sanitized = requireNotNull(NoteCaptureParser.parse(title, "hello"))
        assertFalse(sanitized.title.contains('\n'))
        assertEquals(NoteCaptureParser.MAX_INCOMING_TITLE, sanitized.title.length)
    }

    @Test fun restrictsOversizedBodyWithoutCrashing() {
        val text = "a".repeat(NoteCaptureParser.MAX_INCOMING_BODY + 300)
        val parsed = requireNotNull(NoteCaptureParser.parse("title", text))
        assertEquals(NoteCaptureParser.MAX_INCOMING_BODY, parsed.body.length)
    }

    @Test fun bodyWhitespaceIsNotLostIfNotEntirelyBlank() {
        val payload = NoteCaptureParser.parse("title", "   a   ")
        assertEquals("   a   ", payload?.body)
    }

    @Test fun titleIsOptional() {
        val payload = NoteCaptureParser.parse(null, "hello")
        assertEquals("", payload?.title)
    }

    @Test fun preservesNonLatinText() {
        val parsed = NoteCaptureParser.parse("Idées", "日本語と français")
        assertEquals("日本語と français", parsed?.body)
    }
}
