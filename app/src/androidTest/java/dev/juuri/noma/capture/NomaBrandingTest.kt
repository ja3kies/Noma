package dev.juuri.noma.capture

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.juuri.noma.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Regression guard: the app, shortcut and widget must use the supplied vector brand. */
@RunWith(AndroidJUnit4::class)
class NomaBrandingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun installedApplicationUsesNewAdaptiveLauncherIcon() {
        val info = context.applicationInfo
        assertEquals(R.mipmap.noma_launcher, info.icon)
        // ApplicationInfo does not expose roundIcon on all SDK stubs.
        assertNotNull(context.getDrawable(R.mipmap.noma_launcher_round))
        assertTrue(context.getDrawable(info.icon) is AdaptiveIconDrawable)
    }

    @Test fun adaptiveLayersAndThemedMonochromeExist() {
        assertNotNull(context.getDrawable(R.drawable.noma_launcher_foreground))
        assertNotNull(context.getDrawable(R.drawable.noma_launcher_background))
        assertNotNull(context.getDrawable(R.drawable.noma_launcher_monochrome))
        assertNotNull(context.getDrawable(R.mipmap.noma_launcher_round))
    }

    @Test fun allThreeWidgetStylesKeepBrandVector() {
        for (style in NomaWidgetStyle.entries) {
            val view = android.view.LayoutInflater.from(context).inflate(style.layoutId, null)
            val image = (view as android.view.ViewGroup).getChildAt(0) as android.widget.ImageView
            assertNotNull("Missing widget icon for ${style.name}", image.drawable)
            assertTrue(image.drawable.intrinsicWidth > 0)
        }
    }

    @Test fun existingDrawableAliasStillRenders() {
        assertNotNull(context.getDrawable(R.drawable.ic_noma_launcher))
    }
}
