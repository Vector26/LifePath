package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HabitCompletionResponse {

    @JsonProperty("currency_earned")
    private int currencyEarned;

    private StreakInfo streak;

    public HabitCompletionResponse(int currencyEarned, StreakInfo streak) {
        this.currencyEarned = currencyEarned;
        this.streak = streak;
    }

    public int getCurrencyEarned() { return currencyEarned; }
    public StreakInfo getStreak() { return streak; }

    public static class StreakInfo {
        @JsonProperty("current_streak_days")
        private int currentStreakDays;
        @JsonProperty("longest_streak_days")
        private int longestStreakDays;
        @JsonProperty("last_completed_date")
        private String lastCompletedDate;

        public StreakInfo(int currentStreakDays, int longestStreakDays, String lastCompletedDate) {
            this.currentStreakDays = currentStreakDays;
            this.longestStreakDays = longestStreakDays;
            this.lastCompletedDate = lastCompletedDate;
        }
        public int getCurrentStreakDays() { return currentStreakDays; }
        public int getLongestStreakDays() { return longestStreakDays; }
        public String getLastCompletedDate() { return lastCompletedDate; }
    }
}
