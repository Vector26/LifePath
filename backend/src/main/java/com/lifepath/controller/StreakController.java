package com.lifepath.controller;

import com.lifepath.dto.StreakResponse;
import com.lifepath.model.Habit;
import com.lifepath.model.User;
import com.lifepath.repository.HabitRepository;
import com.lifepath.repository.UserRepository;
import com.lifepath.service.StreakService;
import com.lifepath.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/streaks")
public class StreakController {

    private final StreakService streakService;
    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    public StreakController(StreakService streakService,
                            HabitRepository habitRepository,
                            UserRepository userRepository) {
        this.streakService = streakService;
        this.habitRepository = habitRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<StreakResponse> listStreaks() {
        UUID userId = SecurityUtil.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        return habitRepository.findByUserId(userId).stream()
                .filter(Habit::isStreakTracked)
                .map(h -> {
                    StreakService.StreakResult r = streakService.computeStreak(h.getId(), userId,
                            java.time.ZoneId.of(user.getTimezone()));
                    return new StreakResponse(h.getId(), r.currentStreakDays, r.longestStreakDays,
                            r.lastCompletedDate != null ? r.lastCompletedDate.toString() : null);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{habitId}")
    public StreakResponse getStreak(@PathVariable UUID habitId) {
        UUID userId = SecurityUtil.currentUserId();
        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        StreakService.StreakResult r = streakService.computeStreak(habitId, userId,
                java.time.ZoneId.of(user.getTimezone()));
        return new StreakResponse(habitId, r.currentStreakDays, r.longestStreakDays,
                r.lastCompletedDate != null ? r.lastCompletedDate.toString() : null);
    }
}
