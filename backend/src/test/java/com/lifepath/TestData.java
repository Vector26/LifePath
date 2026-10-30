package com.lifepath;

import com.lifepath.model.*;
import com.lifepath.model.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TestData {

    private TestData() {}

    public static User user(UUID id, String timezone) {
        User u = new User();
        u.setId(id);
        u.setEmail("test@example.com");
        u.setPasswordHash("hash");
        u.setDisplayName("Test User");
        u.setTimezone(timezone != null ? timezone : "UTC");
        u.setTotalXp(0);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }

    public static Quest quest(UUID id, UUID userId, PriorityLevel priority, UUID parentId, QuestStatus status) {
        Quest q = new Quest();
        q.setId(id);
        q.setUserId(userId);
        q.setTitle("Test Quest");
        q.setNotes("notes");
        q.setPriorityLevel(priority != null ? priority : PriorityLevel.P2);
        q.setParentId(parentId);
        q.setStatus(status != null ? status : QuestStatus.pending);
        q.setTimeOfDay(TimeOfDay.any);
        q.setContextTag(null);
        q.setCreatedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        return q;
    }

    public static Quest questWithTime(UUID id, UUID userId, TimeOfDay tod, Instant createdAt) {
        Quest q = quest(id, userId, PriorityLevel.P2, null, QuestStatus.pending);
        q.setTimeOfDay(tod);
        q.setCreatedAt(createdAt);
        q.setUpdatedAt(createdAt);
        return q;
    }

    public static BlockedQuest blockedQuest(UUID questId, Instant unblockedAt) {
        BlockedQuest b = new BlockedQuest();
        b.setQuestId(questId);
        b.setReason("waiting");
        b.setUnblockTrigger(UnblockTrigger.time);
        b.setUnblockAt(Instant.now());
        b.setUnblockedAt(unblockedAt);
        b.setCreatedAt(Instant.now());
        return b;
    }

    public static Habit habit(UUID id, UUID userId, int currency, boolean streakTracked) {
        Habit h = new Habit();
        h.setId(id);
        h.setUserId(userId);
        h.setTitle("Test Habit");
        h.setCurrencyPerCompletion(currency);
        h.setStreakTracked(streakTracked);
        h.setCreatedAt(Instant.now());
        h.setUpdatedAt(Instant.now());
        return h;
    }

    public static HabitCompletion completion(UUID habitId, LocalDate date) {
        HabitCompletion hc = new HabitCompletion();
        hc.setHabitId(habitId);
        hc.setCompletedAt(date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        hc.setCompletionDate(date);
        return hc;
    }
}
