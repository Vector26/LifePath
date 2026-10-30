package com.lifepath.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlin.random.Random

object NotificationScheduler {

    private const val REQUEST_RANDOM = 2101
    private const val REQUEST_OBJECTIVE = 2102

    fun scheduleNotifications(context: Context) {
        showObjectiveNow(context)
        scheduleNextObjectiveReminder(context)
        scheduleNextRandomReminder(context)
    }

    /** Immediately fetch and show the current objective as a permanent notification. */
    fun showObjectiveNow(context: Context) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, NotificationReceiver.TYPE_OBJECTIVE)
        }
        context.sendBroadcast(intent)
    }

    fun scheduleNextObjectiveReminder(context: Context) {
        val minutes = 45 // keep the current objective notification fresh
        val triggerTime = System.currentTimeMillis() + minutes * 60L * 1000L
        scheduleAlarm(context, REQUEST_OBJECTIVE, triggerTime, NotificationReceiver.TYPE_OBJECTIVE)
    }

    fun scheduleNextRandomReminder(context: Context) {
        val minutes = Random.nextInt(75, 240) // 1h15m to 4h
        val triggerTime = System.currentTimeMillis() + minutes * 60L * 1000L
        scheduleAlarm(context, REQUEST_RANDOM, triggerTime, NotificationReceiver.TYPE_RANDOM)
    }

    private fun scheduleAlarm(context: Context, requestCode: Int, triggerTime: Long, type: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(2001, 2002, 2003, REQUEST_RANDOM, REQUEST_OBJECTIVE).forEach { code ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
    }
}
