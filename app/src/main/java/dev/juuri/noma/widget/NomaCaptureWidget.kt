package dev.juuri.noma.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import dev.juuri.noma.MainActivity
import dev.juuri.noma.R

/** No note contents, titles or previews are shown on the home screen. */
class NomaCaptureWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context, manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAll(context, manager, intArrayOf(appWidgetId))
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        updateAll(context, AppWidgetManager.getInstance(context), newWidgetIds)
    }

    private fun updateAll(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { id ->
            val intent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_CREATE_FROM_WIDGET
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pending = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.noma_capture_widget).apply {
                setOnClickPendingIntent(R.id.noma_widget_root, pending)
                setOnClickPendingIntent(R.id.noma_widget_new_note, pending)
                setContentDescription(R.id.noma_widget_new_note, context.getString(R.string.widget_accessibility))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
