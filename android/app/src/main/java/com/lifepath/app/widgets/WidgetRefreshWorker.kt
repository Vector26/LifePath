package com.lifepath.app.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            WidgetDataFetcher.fetchAndCache(applicationContext)
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            refreshWidgets<SmallWidgetProvider>(appWidgetManager)
            refreshWidgets<MediumWidgetProvider>(appWidgetManager)
            refreshWidgets<LargeWidgetProvider>(appWidgetManager)
            Result.success()
        } catch (_: Exception) {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            refreshWidgets<SmallWidgetProvider>(appWidgetManager)
            refreshWidgets<MediumWidgetProvider>(appWidgetManager)
            refreshWidgets<LargeWidgetProvider>(appWidgetManager)
            Result.success()
        }
    }

    private inline fun <reified T : android.appwidget.AppWidgetProvider> refreshWidgets(
        appWidgetManager: AppWidgetManager
    ) {
        val component = ComponentName(applicationContext, T::class.java)
        val ids = appWidgetManager.getAppWidgetIds(component)

        for (id in ids) {
            when (T::class) {
                SmallWidgetProvider::class -> SmallWidgetProvider.updateWidget(applicationContext, appWidgetManager, id)
                MediumWidgetProvider::class -> MediumWidgetProvider.updateWidget(applicationContext, appWidgetManager, id)
                LargeWidgetProvider::class -> LargeWidgetProvider.updateWidget(applicationContext, appWidgetManager, id)
            }
        }
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"
        private const val WORK_IMMEDIATE = "widget_refresh_now"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
