package com.lifepath.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class StreakId implements Serializable {
    private UUID habitId;
    private UUID userId;

    public StreakId() {}

    public StreakId(UUID habitId, UUID userId) {
        this.habitId = habitId;
        this.userId = userId;
    }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreakId streakId = (StreakId) o;
        return Objects.equals(habitId, streakId.habitId) && Objects.equals(userId, streakId.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(habitId, userId);
    }
}
