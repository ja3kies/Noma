package dev.juuri.noma.capture

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun shareTargetIsRegisteredAsPlainTextActivity() {
        val incoming = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage(context.packageName)
        }
        val matches = context.packageManager.queryIntentActivities(
            incoming, PackageManager.MATCH_DEFAULT_ONLY
        )
        assertTrue("Noma must appear in Android text sharesheet", matches.any {
            it.activityInfo.name.endsWith(".MainActivity")
        })
    }

    @Test fun widgetReceiverIsRegisteredWithAppWidgetMetadata() {
        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context, NomaQuickCaptureWidget::class.java),
            PackageManager.GET_META_DATA
        )
        assertTrue(receiver.exported)
        assertNotNull(receiver.metaData)
        assertTrue(receiver.metaData.getInt(
            AppWidgetManager.META_DATA_APPWIDGET_PROVIDER
        ) != 0)
    }

    @Test fun widgetResourcesArePresentForCurrentTheme() {
        assertTrue(context.getString(
            dev.juuri.noma.R.string.widget_description
        ).isNotBlank())
        assertTrue(context.resources.getIdentifier(
            "noma_quick_capture_widget", "layout", context.packageName
        ) != 0)
    }
}
