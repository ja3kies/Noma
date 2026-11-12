package dev.juuri.noma.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Plain-text list marker on the caret's paragraph; no rich-text storage implied. */
internal fun toggleBulletLine(value: TextFieldValue): TextFieldValue {
    val caret = value.selection.min
    val start = if (caret == 0) 0 else value.text.lastIndexOf('\n', caret - 1) + 1
    val removing = value.text.startsWith("- ", start)
    val text = if (removing) value.text.removeRange(start, start + 2)
        else value.text.substring(0, start) + "- " + value.text.substring(start)
    fun offset(position: Int): Int = if (removing) (position - 2).coerceAtLeast(start)
        else position + 2
    return TextFieldValue(text, TextRange(offset(value.selection.start), offset(value.selection.end)))
}
