package dev.juuri.noma.domain

import org.junit.Assert.*
import org.junit.Test

class ChecklistSyntaxTest {
    @Test fun readsUncheckedAndCheckedItems() {
        val found = ChecklistSyntax.items("- [ ] Shop\n- [x] Pack\n* [X] Leave")
        assertEquals(3, found.size)
        assertEquals(listOf(false, true, true), found.map { it.checked })
        assertEquals(listOf("Shop", "Pack", "Leave"), found.map { it.text })
    }

    @Test fun ignoresPlainBulletLines() {
        assertTrue(ChecklistSyntax.items("- plain\nno checkbox").isEmpty())
    }

    @Test fun togglesOneTaskWithoutMutatingOtherLines() {
        assertEquals("- [x] One\n- [ ] Two",
            ChecklistSyntax.toggle("- [ ] One\n- [ ] Two", 0))
    }

    @Test fun completedTaskCanBeUnchecked() {
        assertEquals("- [ ] Task", ChecklistSyntax.toggle("- [x] Task", 0))
    }

    @Test fun handlesUnicodeTasks() {
        assertEquals("Étudier 日本語", ChecklistSyntax.items("- [ ] Étudier 日本語").single().text)
    }

    @Test fun outOfBoundsToggleDoesNotDamageText() {
        assertEquals("- [ ] One", ChecklistSyntax.toggle("- [ ] One", 500))
    }

    @Test fun nonChecklistToggleDoesNotDamageText() {
        assertEquals("hello", ChecklistSyntax.toggle("hello", 0))
    }

    @Test fun insertingFirstTaskProducesValidMarkdown() {
        assertEquals("- [ ] ", ChecklistSyntax.append(""))
    }

    @Test fun insertingTaskAfterParagraphKeepsParagraph() {
        assertEquals("Journal\n- [ ] ", ChecklistSyntax.append("Journal"))
    }

    @Test fun insertingTaskAfterNewlineDoesNotCreateExtraLine() {
        assertEquals("Journal\n- [ ] ", ChecklistSyntax.append("Journal\n"))
    }

    @Test fun togglingLineRetainsRestOfDocument() {
        val original = "Heading\n- [ ] A\nFooter\n"
        assertEquals("Heading\n- [x] A\nFooter\n", ChecklistSyntax.toggle(original, 1))
    }
}
