package dev.juuri.noma.capture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
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
        val style = NomaWidgetPreferences.read(context)
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
            val views = RemoteViews(context.packageName, style.layoutId)
            views.setOnClickPendingIntent(R.id.noma_widget_capture, pending)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        /** Refresh installed instances after a user preference or system theme change. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, NomaQuickCaptureWidget::class.java)
            )
            if (ids.isNotEmpty()) {
                NomaQuickCaptureWidget().onUpdate(context, manager, ids)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Cached RemoteViews still reference the previous APK's resources
            // until each installed instance receives a complete refresh.
            refreshAll(context)
        }
    }
}
