package dev.juuri.noma.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class NomaTextEditingTest {
    @Test fun emptyParagraphBecomesEditableBullet() {
        assertEquals(TextFieldValue("- ", TextRange(2)), toggleBulletLine(TextFieldValue("")))
    }
    @Test fun onlyCaretParagraphChangesAndSelectionIsPreserved() {
        val original = TextFieldValue("First\nCafé 🌿\nLast", TextRange(11, 7))
        val changed = toggleBulletLine(original)
        assertEquals("First\n- Café 🌿\nLast", changed.text)
        assertEquals(TextRange(13, 9), changed.selection)
        assertEquals(original, toggleBulletLine(changed))
    }
    @Test fun removingMarkerClampsCaretInsideMarker() {
        assertEquals(TextFieldValue("hello", TextRange(0)),
            toggleBulletLine(TextFieldValue("- hello", TextRange(1))))
    }
    @Test fun trailingNewlineCreatesBulletOnEmptyLastParagraph() {
        assertEquals(TextFieldValue("a\n- ", TextRange(4)),
            toggleBulletLine(TextFieldValue("a\n", TextRange(2))))
    }
    @Test fun carriageReturnsAndOtherParagraphsRemainUntouched() {
        assertEquals("first\r\n- second", toggleBulletLine(
            TextFieldValue("first\r\nsecond", TextRange(9))).text)
    }
}
