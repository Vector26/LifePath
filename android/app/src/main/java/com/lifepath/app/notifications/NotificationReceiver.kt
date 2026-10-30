package com.lifepath.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifepath.app.auth.ApiClient
import org.json.JSONObject
import kotlin.concurrent.thread

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TYPE = "notification_type"
        const val TYPE_OBJECTIVE = "objective"
        const val TYPE_HABIT = "habit"
        const val TYPE_SUMMARY = "summary"
        const val TYPE_RANDOM = "random_pending"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return

        thread {
            try {
                when (type) {
                    TYPE_OBJECTIVE -> fetchAndNotifyObjective(context)
                    TYPE_HABIT -> notifyHabitPrompt(context)
                    TYPE_SUMMARY -> fetchAndNotifySummary(context)
                    TYPE_RANDOM -> fetchAndNotifyRandomPending(context)
                }
            } catch (e: Exception) {
                showFallbackNotification(context, type)
            } finally {
                if (type == TYPE_RANDOM) {
                    NotificationScheduler.scheduleNextRandomReminder(context)
                } else if (type == TYPE_OBJECTIVE) {
                    NotificationScheduler.scheduleNextObjectiveReminder(context)
                }
            }
        }
    }

    private fun fetchAndNotifyRandomPending(context: Context) {
        val pendingQuests = runCatching {
            val questsBody = ApiClient.get(context, "/quests?status=pending&limit=100")
            val list = org.json.JSONArray(questsBody)
            list.length()
        }.getOrDefault(0)

        val habitsLeft = runCatching {
            val habitsBody = ApiClient.get(context, "/habits")
            val habitsTotal = org.json.JSONArray(habitsBody).length()
            val statsBody = ApiClient.get(context, "/stats/today")
            val completedToday = JSONObject(statsBody).optInt("habits_completed_today", 0)
            (habitsTotal - completedToday).coerceAtLeast(0)
        }.getOrDefault(0)

        if (pendingQuests <= 0 && habitsLeft <= 0) return

        val lines = mutableListOf<String>()
        if (pendingQuests > 0) lines += "$pendingQuests quest${if (pendingQuests == 1) "" else "s"} waiting"
        if (habitsLeft > 0) lines += "$habitsLeft habit${if (habitsLeft == 1) "" else "s"} left today"
        val body = "Hey — ${lines.joinToString(" · ")}."

        NotificationHelper.showNotification(
            context,
            NotificationHelper.CHANNEL_OBJECTIVES,
            "LifePath check-in",
            body,
            3010,
            "/quest"
        )
    }

    private fun fetchAndNotifyObjective(context: Context) {
        val body = ApiClient.get(context, "/objectives/current?context=focus")
        if (body.isNotBlank()) {
            val json = JSONObject(body)
            val type = json.optString("type", "quest")
            val payload = json.optJSONObject("payload")
            val title = when (type) {
                "quest" -> payload?.optJSONObject("quest")?.optString("title", "your quest") ?: "your quest"
                "habit" -> payload?.optString("title", "your habit") ?: "your habit"
                "calendar_event" -> payload?.optString("title", "your event") ?: "your event"
                else -> "your next task"
            }

            NotificationHelper.showNotification(
                context,
                NotificationHelper.CHANNEL_OBJECTIVES,
                "Your current focus",
                "Your current focus: $title",
                3001,
                "/quest",
                ongoing = true
            )
        }
    }

    private fun notifyHabitPrompt(context: Context) {
        NotificationHelper.showNotification(
            context,
            NotificationHelper.CHANNEL_HABITS,
            "Habit Reminder",
            "Don't forget to check your habits today!",
            3002,
            "/habits"
        )
    }

    private fun fetchAndNotifySummary(context: Context) {
        val body = ApiClient.get(context, "/stats/today")
        if (body.isNotBlank()) {
            val json = JSONObject(body)
            val quests = json.optInt("quests_completed_today", 0)
            val habits = json.optInt("habits_completed_today", 0)

            NotificationHelper.showNotification(
                context,
                NotificationHelper.CHANNEL_SUMMARY,
                "Daily Summary",
                "$quests quests, $habits habits completed today",
                3003,
                "/quest"
            )
        }
    }

    private fun showFallbackNotification(context: Context, type: String) {
        when (type) {
            TYPE_OBJECTIVE -> NotificationHelper.showNotification(
                context, NotificationHelper.CHANNEL_OBJECTIVES,
                "Stay focused", "Check your current objective in LifePath", 3001, "/quest", ongoing = true
            )
            TYPE_HABIT -> NotificationHelper.showNotification(
                context, NotificationHelper.CHANNEL_HABITS,
                "Habit Reminder", "Don't forget your habits today!", 3002, "/habits"
            )
            TYPE_SUMMARY -> NotificationHelper.showNotification(
                context, NotificationHelper.CHANNEL_SUMMARY,
                "Daily Summary", "Check your progress for today", 3003, "/quest"
            )
            TYPE_RANDOM -> NotificationHelper.showNotification(
                context, NotificationHelper.CHANNEL_OBJECTIVES,
                "LifePath check-in", "You still have tasks left today.", 3010, "/quest"
            )
        }
    }
}
