package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TodayStatsResponse {

    @JsonProperty("quests_completed_today")
    private long questsCompletedToday;

    @JsonProperty("habits_completed_today")
    private long habitsCompletedToday;

    private String date;

    public TodayStatsResponse(long questsCompletedToday, long habitsCompletedToday, String date) {
        this.questsCompletedToday = questsCompletedToday;
        this.habitsCompletedToday = habitsCompletedToday;
        this.date = date;
    }

    public long getQuestsCompletedToday() { return questsCompletedToday; }
    public long getHabitsCompletedToday() { return habitsCompletedToday; }
    public String getDate() { return date; }
}
