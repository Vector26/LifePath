package com.lifepath.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streaks")
@IdClass(StreakId.class)
public class Streak {

    @Id
    @Column(name = "habit_id")
    private UUID habitId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "current_streak_days", nullable = false)
    private int currentStreakDays = 0;

    @Column(name = "longest_streak_days", nullable = false)
    private int longestStreakDays = 0;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }
    public int getLongestStreakDays() { return longestStreakDays; }
    public void setLongestStreakDays(int longestStreakDays) { this.longestStreakDays = longestStreakDays; }
    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(LocalDate lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }
}
