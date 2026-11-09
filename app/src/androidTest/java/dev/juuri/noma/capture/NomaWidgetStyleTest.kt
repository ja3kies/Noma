package dev.juuri.noma.capture

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.juuri.noma.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomaWidgetStyleTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun unknownAndMissingSettingsUseMaterialYou() {
        assertEquals(NomaWidgetStyle.MATERIAL_YOU, NomaWidgetStyle.fromStorage(null))
        assertEquals(NomaWidgetStyle.MATERIAL_YOU, NomaWidgetStyle.fromStorage("LEGACY_OR_INVALID"))
    }

    @Test fun allStylesRoundTripThroughStorageNames() {
        for (style in NomaWidgetStyle.entries) {
            assertEquals(style, NomaWidgetStyle.fromStorage(style.name))
        }
    }

    @Test fun choicePersistsUntilExplicitlyChanged() {
        val original = NomaWidgetPreferences.read(context)
        try {
            for (style in NomaWidgetStyle.entries) {
                NomaWidgetPreferences.save(context, style)
                assertEquals(style, NomaWidgetPreferences.read(context))
            }
        } finally {
            NomaWidgetPreferences.save(context, original)
        }
    }

    @Test fun everyVariantHasVisibleTapTargetAndLocalizedText() {
        for (style in NomaWidgetStyle.entries) {
            val view = LayoutInflater.from(context).inflate(style.layoutId, null, false)
            assertNotNull(view.findViewById<View>(R.id.noma_widget_capture))
            assertEquals(
                context.getString(R.string.shortcut_new_note_detail),
                view.contentDescription?.toString()
            )
            val children = (view as android.view.ViewGroup)
            assertTrue(children.childCount >= 2)
            assertTrue((children.getChildAt(1) as TextView).text.isNotBlank())
        }
    }

    @Test fun allLayoutsUseDistinctResources() {
        val layouts = NomaWidgetStyle.entries.map { it.layoutId }
        assertEquals(3, layouts.size)
        assertEquals(3, layouts.distinct().size)
    }

    @Test fun highContrastTextResourcesExist() {
        val colors = listOf(
            R.color.noma_widget_material_text,
            R.color.noma_widget_frosted_text,
            R.color.noma_widget_glass_text
        )
        colors.forEach { assertTrue(context.getColor(it) != 0) }
    }
}
