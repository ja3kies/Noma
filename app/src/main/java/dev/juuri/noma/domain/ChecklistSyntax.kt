package dev.juuri.noma.domain

/** Plain-text Markdown tasks are readable and portable in .md files and backups. */
data class ChecklistItem(val lineIndex: Int, val checked: Boolean, val text: String)

object ChecklistSyntax {
    private val row = Regex("""^(\\s*[-*]\\s+\\[)([ xX])(\\]\\s*)(.*)$""")

    fun items(body: String): List<ChecklistItem> =
        body.split('\n').mapIndexedNotNull { index, line ->
            val match = row.matchEntire(line.trimEnd('\r')) ?: return@mapIndexedNotNull null
            ChecklistItem(
                lineIndex = index,
                checked = match.groupValues[2].equals("x", ignoreCase = true),
                text = match.groupValues[4]
            )
        }

    fun append(body: String): String = when {
        body.isEmpty() -> "- [ ] "
        body.endsWith("\n") -> body + "- [ ] "
        else -> body + "\n- [ ] "
    }

    fun toggle(body: String, targetLine: Int): String {
        val lines = body.split('\n').toMutableList()
        if (targetLine !in lines.indices) return body
        val line = lines[targetLine]
        val match = row.matchEntire(line.trimEnd('\r')) ?: return body
        val target = match.groups[2]?.range ?: return body
        val isChecked = match.groupValues[2].equals("x", ignoreCase = true)
        lines[targetLine] = line.replaceRange(target, if (isChecked) " " else "x")
        return lines.joinToString("\n")
    }
}
