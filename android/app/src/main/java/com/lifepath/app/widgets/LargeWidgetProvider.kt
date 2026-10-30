package com.lifepath.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lifepath.app.MainActivity
import com.lifepath.app.R

class LargeWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_large)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            val data = WidgetDataFetcher.getFromCache(context)
            if (data.hasData) {
                val type = data.objectiveType.ifBlank { "focus" }.replace('_', ' ')
                views.setTextViewText(R.id.objectiveType, type.uppercase())
                views.setTextViewText(R.id.objectiveTitle, data.objectiveTitle)
                views.setTextViewText(R.id.questCount, "${data.questsCompleted}")
                views.setTextViewText(R.id.questLabel, "Quests done")
                views.setTextViewText(R.id.habitCount, "${data.habitsCompleted}")
                views.setTextViewText(R.id.habitLabel, "Habits done")
                views.setTextViewText(R.id.totalCount, "${data.questsCompleted + data.habitsCompleted}")
                views.setTextViewText(R.id.totalLabel, "Total today")
                views.setTextViewText(R.id.queueHint, "${data.queueCount} in queue")
                views.setViewVisibility(R.id.queueHint, if (data.queueCount > 0) android.view.View.VISIBLE else android.view.View.GONE)
                if (data.topStreakDays > 0) {
                    views.setTextViewText(R.id.streakHint, "Best streak: ${data.topStreakDays} days")
                    views.setViewVisibility(R.id.streakHint, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.streakHint, android.view.View.GONE)
                }
            } else {
                views.setTextViewText(R.id.objectiveType, "CONNECT")
                views.setTextViewText(R.id.objectiveTitle, "Connect to see your progress")
                views.setTextViewText(R.id.questCount, "0")
                views.setTextViewText(R.id.habitCount, "0")
                views.setTextViewText(R.id.totalCount, "0")
                views.setViewVisibility(R.id.queueHint, android.view.View.GONE)
                views.setViewVisibility(R.id.streakHint, android.view.View.GONE)
            }
            views.setOnClickPendingIntent(R.id.openButton, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
