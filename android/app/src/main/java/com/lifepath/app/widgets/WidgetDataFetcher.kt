package com.lifepath.app.widgets

import android.content.Context
import com.lifepath.app.auth.ApiClient
import org.json.JSONObject

data class WidgetData(
    val objectiveTitle: String,
    val objectiveType: String,
    val questsCompleted: Int,
    val habitsCompleted: Int,
    val queueCount: Int = 0,
    val topStreakDays: Int = 0,
    val hasData: Boolean
)

object WidgetDataFetcher {

    fun getFromCache(context: Context): WidgetData {
        return WidgetCache.getCached(context) ?: emptyData()
    }

    fun fetchAndCache(context: Context): WidgetData {
        val data = fetchFromApi(context)
        WidgetCache.put(context, data)
        return data
    }

    private fun fetchFromApi(context: Context): WidgetData {
        var objectiveTitle = "No current objective"
        var objectiveType = ""
        var questsCompleted = 0
        var habitsCompleted = 0
        var queueCount = 0
        var topStreakDays = 0

        try {
            val body = ApiClient.get(context, "/quests?status=pending&limit=50")
            if (body.isNotBlank() && body.startsWith("[")) {
                val arr = org.json.JSONArray(body)
                queueCount = arr.length()
            }
        } catch (_: Exception) {}

        try {
            val body = ApiClient.get(context, "/objectives/current?context=focus")
            if (body.isNotBlank()) {
                val json = JSONObject(body)
                objectiveType = json.optString("type", "quest")
                val payload = json.optJSONObject("payload")
                objectiveTitle = when (objectiveType) {
                    "quest" -> payload?.optJSONObject("quest")?.optString("title", "Quest") ?: "Quest"
                    "habit" -> payload?.optString("title", "Habit") ?: "Habit"
                    "calendar_event" -> payload?.optString("title", "Event") ?: "Event"
                    else -> "Task"
                }
            }
        } catch (_: Exception) {}

        try {
            val body = ApiClient.get(context, "/stats/today")
            if (body.isNotBlank()) {
                val json = JSONObject(body)
                questsCompleted = json.optInt("quests_completed_today", 0)
                habitsCompleted = json.optInt("habits_completed_today", 0)
            }
        } catch (_: Exception) {}

        try {
            val body = ApiClient.get(context, "/streaks")
            if (body.isNotBlank() && body.startsWith("[")) {
                val streaks = org.json.JSONArray(body)
                for (i in 0 until streaks.length()) {
                    val entry = streaks.optJSONObject(i) ?: continue
                    val current = entry.optInt("current_streak_days", 0)
                    if (current > topStreakDays) topStreakDays = current
                }
            }
        } catch (_: Exception) {}

        return WidgetData(
            objectiveTitle = objectiveTitle,
            objectiveType = objectiveType,
            questsCompleted = questsCompleted,
            habitsCompleted = habitsCompleted,
            queueCount = queueCount,
            topStreakDays = topStreakDays,
            hasData = true
        )
    }

    private fun emptyData() = WidgetData(
        objectiveTitle = "Open app to connect",
        objectiveType = "",
        questsCompleted = 0,
        habitsCompleted = 0,
        queueCount = 0,
        topStreakDays = 0,
        hasData = false
    )
}
