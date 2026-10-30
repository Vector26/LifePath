package com.lifepath.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lifepath.app.MainActivity
import com.lifepath.app.R

object NotificationHelper {

    const val CHANNEL_OBJECTIVES = "objectives"
    const val CHANNEL_HABITS = "habits"
    const val CHANNEL_SUMMARY = "summary"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val objectivesChannel = NotificationChannel(
                CHANNEL_OBJECTIVES,
                "Objective Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders about your current objective"
            }

            val habitsChannel = NotificationChannel(
                CHANNEL_HABITS,
                "Habit Prompts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to complete your habits"
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily progress summary"
            }

            manager.createNotificationChannels(
                listOf(objectivesChannel, habitsChannel, summaryChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        notificationId: Int,
        deepLink: String? = null,
        ongoing: Boolean = false
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!deepLink.isNullOrBlank()) {
                data = android.net.Uri.parse("lifepath://$deepLink")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
