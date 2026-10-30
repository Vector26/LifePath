package com.lifepath.service;

import com.lifepath.model.HabitCompletion;
import com.lifepath.model.Streak;
import com.lifepath.repository.HabitCompletionRepository;
import com.lifepath.repository.StreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StreakService {

    private final HabitCompletionRepository habitCompletionRepository;
    private final StreakRepository streakRepository;

    public StreakService(HabitCompletionRepository habitCompletionRepository,
                         StreakRepository streakRepository) {
        this.habitCompletionRepository = habitCompletionRepository;
        this.streakRepository = streakRepository;
    }

    public StreakResult computeStreak(UUID habitId, UUID userId, ZoneId userZone) {
        List<HabitCompletion> completions = habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId);
        Set<LocalDate> dates = completions.stream()
                .map(hc -> hc.getCompletedAt().atZone(userZone).toLocalDate())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<LocalDate> sortedDates = new ArrayList<>(dates);
        sortedDates.sort(Comparator.reverseOrder());

        if (sortedDates.isEmpty()) {
            return new StreakResult(0, 0, null);
        }

        int longest = 0;
        int currentRun = 0;
        LocalDate prevDate = null;
        for (LocalDate d : sortedDates) {
            if (prevDate == null) {
                currentRun = 1;
            } else if (ChronoUnit.DAYS.between(d, prevDate) == 1) {
                currentRun += 1;
            } else {
                longest = Math.max(longest, currentRun);
                currentRun = 1;
            }
            prevDate = d;
        }
        longest = Math.max(longest, currentRun);

        LocalDate mostRecent = sortedDates.get(0);
        LocalDate today = LocalDate.now(userZone);
        int currentStreak = 0;
        if (mostRecent.equals(today) || mostRecent.equals(today.minusDays(1))) {
            currentStreak = 1;
            prevDate = mostRecent;
            for (int i = 1; i < sortedDates.size(); i++) {
                LocalDate d = sortedDates.get(i);
                if (ChronoUnit.DAYS.between(d, prevDate) == 1) {
                    currentStreak++;
                    prevDate = d;
                } else {
                    break;
                }
            }
        }

        return new StreakResult(currentStreak, longest, mostRecent);
    }

    @Transactional
    public StreakResult updateStreak(UUID habitId, UUID userId, ZoneId userZone) {
        StreakResult result = computeStreak(habitId, userId, userZone);
        Streak streak = streakRepository.findByHabitIdAndUserId(habitId, userId).orElseGet(() -> {
            Streak s = new Streak();
            s.setHabitId(habitId);
            s.setUserId(userId);
            return s;
        });
        streak.setCurrentStreakDays(result.currentStreakDays);
        streak.setLongestStreakDays(Math.max(streak.getLongestStreakDays(), result.longestStreakDays));
        streak.setLastCompletedDate(result.lastCompletedDate);
        streakRepository.save(streak);
        return result;
    }

    public static class StreakResult {
        public final int currentStreakDays;
        public final int longestStreakDays;
        public final LocalDate lastCompletedDate;

        public StreakResult(int currentStreakDays, int longestStreakDays, LocalDate lastCompletedDate) {
            this.currentStreakDays = currentStreakDays;
            this.longestStreakDays = longestStreakDays;
            this.lastCompletedDate = lastCompletedDate;
        }
    }
}
