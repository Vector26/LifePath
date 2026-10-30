package com.lifepath.service;

import com.lifepath.dto.TodayStatsResponse;
import com.lifepath.model.Habit;
import com.lifepath.repository.HabitCompletionRepository;
import com.lifepath.repository.HabitRepository;
import com.lifepath.repository.QuestRepository;
import com.lifepath.repository.UserRepository;
import com.lifepath.model.enums.QuestStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final QuestRepository questRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final UserRepository userRepository;

    public StatsService(QuestRepository questRepository,
                        HabitRepository habitRepository,
                        HabitCompletionRepository habitCompletionRepository,
                        UserRepository userRepository) {
        this.questRepository = questRepository;
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.userRepository = userRepository;
    }

    public TodayStatsResponse getTodayStats(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    ZoneId zone = ZoneId.of(user.getTimezone());
                    LocalDate today = LocalDate.now(zone);
                    Instant dayStart = today.atStartOfDay(zone).toInstant();
                    Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

                    long quests = questRepository.countByUserIdAndStatusAndCompletedAtBetween(
                            userId, QuestStatus.completed, dayStart, dayEnd);

                    Set<UUID> habitIds = habitRepository.findByUserId(userId).stream()
                            .map(Habit::getId)
                            .collect(Collectors.toSet());
                    long habits = habitIds.isEmpty()
                            ? 0
                            : habitCompletionRepository.countByHabitIdInAndCompletionDate(habitIds, today);

                    return new TodayStatsResponse(quests, habits, today.toString());
                })
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
    }
}
