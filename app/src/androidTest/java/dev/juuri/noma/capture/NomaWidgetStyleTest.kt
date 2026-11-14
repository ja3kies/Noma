package dev.juuri.noma.capture

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import android.content.Intent
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

    @Test fun widgetProviderReceivesOwnPackageReplacementBroadcast() {
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryBroadcastReceivers(intent, 0)
        assertTrue(matches.any {
            it.activityInfo.name == NomaQuickCaptureWidget::class.java.name
        })
    }

    @Test fun packageReplacementSafelyRefreshesWithNoInstalledWidgets() {
        NomaQuickCaptureWidget().onReceive(context,
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

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
            R.color.noma_widget_material_background,
            R.color.noma_widget_material_outline,
            R.color.noma_widget_frosted_text,
            R.color.noma_widget_frosted_start,
            R.color.noma_widget_frosted_center,
            R.color.noma_widget_frosted_end,
            R.color.noma_widget_frosted_stroke,
            R.color.noma_widget_glass_text,
            R.color.noma_widget_glass_fill,
            R.color.noma_widget_glass_outer_stroke,
            R.color.noma_widget_glass_stroke
        )
        colors.forEach { assertTrue(context.getColor(it) != 0) }
    }

    @Test fun widgetLabelsMeetBodyTextContrastInBothModesAndWallpaperExtremes() {
        for (nightMode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val config = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            val themed = context.createConfigurationContext(config)
            val palettes = listOf(
                R.color.noma_widget_material_text to listOf(R.color.noma_widget_material_background),
                R.color.noma_widget_frosted_text to listOf(
                    R.color.noma_widget_frosted_start,
                    R.color.noma_widget_frosted_center,
                    R.color.noma_widget_frosted_end
                ),
                R.color.noma_widget_glass_text to listOf(R.color.noma_widget_glass_fill)
            )
            for ((foregroundRes, backgroundResources) in palettes) {
                val foreground = themed.getColor(foregroundRes)
                for (backgroundRes in backgroundResources) {
                    val translucentBackground = themed.getColor(backgroundRes)
                    for (wallpaper in listOf(Color.BLACK, Color.WHITE)) {
                        val effectiveBackground = ColorUtils.compositeColors(translucentBackground, wallpaper)
                        val contrast = ColorUtils.calculateContrast(foreground, effectiveBackground)
                        assertTrue(
                            "night=$nightMode fg=$foregroundRes bg=$backgroundRes wallpaper=$wallpaper ratio=$contrast",
                            contrast >= 4.5
                        )
                    }
                }
            }
        }
    }

    @Test fun captureLabelFitsSmallestSupportedWidgetWidth() {
        val width = (180 * context.resources.displayMetrics.density).toInt()
        val height = (56 * context.resources.displayMetrics.density).toInt()
        for (style in NomaWidgetStyle.entries) {
            val view = LayoutInflater.from(context).inflate(style.layoutId, null, false)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, width, height)
            val label = (view as android.view.ViewGroup).getChildAt(1) as TextView
            assertEquals("${style.name} at 180dp", 0, label.layout.getEllipsisCount(0))
        }
    }
}
