package com.lifepath.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lifepath.app.MainActivity
import com.lifepath.app.R

class SmallWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.refreshNow(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_small)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            views.setOnClickPendingIntent(R.id.openButton, pendingIntent)

            val data = WidgetDataFetcher.getFromCache(context)
            if (data.hasData) {
                val type = data.objectiveType.ifBlank { "focus" }.replace('_', ' ')
                views.setTextViewText(R.id.objectiveType, type.uppercase())
                views.setViewVisibility(R.id.objectiveType, android.view.View.VISIBLE)
                views.setTextViewText(R.id.objectiveTitle, data.objectiveTitle)
                views.setTextViewText(R.id.metricText, "${data.questsCompleted + data.habitsCompleted} today")
            } else {
                views.setViewVisibility(R.id.objectiveType, android.view.View.GONE)
                views.setTextViewText(R.id.objectiveTitle, "Open LifePath")
                views.setTextViewText(R.id.metricText, "Tap to connect")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
