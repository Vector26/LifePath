package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class StreakResponse {

    private UUID habitId;

    @JsonProperty("current_streak_days")
    private int currentStreakDays;

    @JsonProperty("longest_streak_days")
    private int longestStreakDays;

    @JsonProperty("last_completed_date")
    private String lastCompletedDate;

    public StreakResponse(UUID habitId, int currentStreakDays, int longestStreakDays, String lastCompletedDate) {
        this.habitId = habitId;
        this.currentStreakDays = currentStreakDays;
        this.longestStreakDays = longestStreakDays;
        this.lastCompletedDate = lastCompletedDate;
    }

    public UUID getHabitId() { return habitId; }
    public int getCurrentStreakDays() { return currentStreakDays; }
    public int getLongestStreakDays() { return longestStreakDays; }
    public String getLastCompletedDate() { return lastCompletedDate; }
}
