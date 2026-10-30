package com.lifepath.app.widgets

import android.content.Context
import org.json.JSONObject

object WidgetCache {

    private const val PREF_NAME = "lifepath_widget_cache"
    private const val KEY_DATA = "cache_data"
    private const val KEY_UPDATED_AT = "cache_updated_at"

    fun getCached(context: Context): WidgetData? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null) ?: return null
        return try {
            val obj = JSONObject(json)
            WidgetData(
                objectiveTitle = obj.optString("objectiveTitle", "No current objective"),
                objectiveType = obj.optString("objectiveType", ""),
                questsCompleted = obj.optInt("questsCompleted", 0),
                habitsCompleted = obj.optInt("habitsCompleted", 0),
                queueCount = obj.optInt("queueCount", 0),
                topStreakDays = obj.optInt("topStreakDays", 0),
                hasData = obj.optBoolean("hasData", true)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun put(context: Context, data: WidgetData) {
        val json = JSONObject().apply {
            put("objectiveTitle", data.objectiveTitle)
            put("objectiveType", data.objectiveType)
            put("questsCompleted", data.questsCompleted)
            put("habitsCompleted", data.habitsCompleted)
            put("queueCount", data.queueCount)
            put("topStreakDays", data.topStreakDays)
            put("hasData", data.hasData)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATA, json.toString())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
