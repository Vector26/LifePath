package com.lifepath.service;

import com.lifepath.model.HabitCompletion;
import com.lifepath.repository.HabitCompletionRepository;
import com.lifepath.repository.StreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lifepath.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock HabitCompletionRepository habitCompletionRepository;
    @Mock StreakRepository streakRepository;

    StreakService service;

    UUID habitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ZoneId utc = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        service = new StreakService(habitCompletionRepository, streakRepository);
    }

    @Nested
    @DisplayName("Empty completions")
    class Empty {

        @Test
        void returnsZeroWhenNoCompletions() {
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(List.of());

            var result = service.computeStreak(habitId, userId, utc);
            assertThat(result.currentStreakDays).isZero();
            assertThat(result.longestStreakDays).isZero();
            assertThat(result.lastCompletedDate).isNull();
        }
    }

    @Nested
    @DisplayName("Consecutive days")
    class Consecutive {

        @Test
        void singleDayStreakIs1() {
            LocalDate today = LocalDate.now(utc);
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(List.of(completion(habitId, today)));

            var result = service.computeStreak(habitId, userId, utc);
            assertThat(result.currentStreakDays).isEqualTo(1);
            assertThat(result.longestStreakDays).isEqualTo(1);
            assertThat(result.lastCompletedDate).isEqualTo(today);
        }

        @Test
        void threeConsecutiveDays() {
            LocalDate today = LocalDate.now(utc);
            LocalDate yesterday = today.minusDays(1);
            LocalDate twoDaysAgo = today.minusDays(2);
            List<HabitCompletion> completions = List.of(
                    completion(habitId, today),
                    completion(habitId, yesterday),
                    completion(habitId, twoDaysAgo)
            );
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(completions);

            var result = service.computeStreak(habitId, userId, utc);
            assertThat(result.currentStreakDays).isEqualTo(3);
            assertThat(result.longestStreakDays).isEqualTo(3);
            assertThat(result.lastCompletedDate).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("Gap breaks streak")
    class Gap {

        @Test
        void gapResetsCurrentStreak() {
            LocalDate today = LocalDate.now(utc);
            LocalDate twoDaysAgo = today.minusDays(2);
            List<HabitCompletion> completions = List.of(
                    completion(habitId, today),
                    completion(habitId, twoDaysAgo)
            );
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(completions);

            var result = service.computeStreak(habitId, userId, utc);
            assertThat(result.currentStreakDays).isEqualTo(1);
            assertThat(result.longestStreakDays).isEqualTo(1);
        }

        @Test
        void oldStreakOnly_updatesLongest() {
            LocalDate weekAgo = LocalDate.now(utc).minusDays(7);
            LocalDate sixDaysAgo = weekAgo.minusDays(1);
            List<HabitCompletion> completions = List.of(
                    completion(habitId, weekAgo),
                    completion(habitId, sixDaysAgo)
            );
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(completions);

            var result = service.computeStreak(habitId, userId, utc);
            assertThat(result.currentStreakDays).isZero();
            assertThat(result.longestStreakDays).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Update streak")
    class Update {

        @Test
        void createsStreakWhenNotExists() {
            LocalDate today = LocalDate.now(utc);
            when(habitCompletionRepository.findByHabitIdOrderByCompletedAtDesc(habitId))
                    .thenReturn(List.of(completion(habitId, today)));
            when(streakRepository.findByHabitIdAndUserId(habitId, userId)).thenReturn(Optional.empty());

            var result = service.updateStreak(habitId, userId, utc);

            assertThat(result.currentStreakDays).isEqualTo(1);
            verify(streakRepository).save(any());
        }
    }
}
