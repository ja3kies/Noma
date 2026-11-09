package dev.juuri.noma.capture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.juuri.noma.MainActivity
import dev.juuri.noma.R

/** A one-tap entry point; no background service, note reads or permissions. */
class NomaQuickCaptureWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val capture = Intent(context, MainActivity::class.java).apply {
                action = AndroidCaptureIntents.ACTION_NEW_NOTE
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pending = PendingIntent.getActivity(
                context,
                widgetId,
                capture,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.noma_quick_capture_widget)
            views.setOnClickPendingIntent(R.id.noma_widget_capture, pending)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
