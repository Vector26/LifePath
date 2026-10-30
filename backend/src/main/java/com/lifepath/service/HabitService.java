package com.lifepath.service;

import com.lifepath.dto.CreateHabitRequest;
import com.lifepath.dto.HabitCompletionResponse;
import com.lifepath.dto.HabitResponse;
import com.lifepath.dto.UpdateHabitRequest;
import com.lifepath.model.*;
import com.lifepath.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final UserRepository userRepository;
    private final CityStateRepository cityStateRepository;
    private final StreakService streakService;

    public HabitService(HabitRepository habitRepository,
                        HabitCompletionRepository habitCompletionRepository,
                        UserRepository userRepository,
                        CityStateRepository cityStateRepository,
                        StreakService streakService) {
        this.habitRepository = habitRepository;
        this.habitCompletionRepository = habitCompletionRepository;
        this.userRepository = userRepository;
        this.cityStateRepository = cityStateRepository;
        this.streakService = streakService;
    }

    public List<HabitResponse> listHabits(UUID userId) {
        return habitRepository.findByUserId(userId).stream()
                .map(HabitResponse::from)
                .collect(Collectors.toList());
    }

    public HabitResponse getHabit(UUID id, UUID userId) {
        Habit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));
        return HabitResponse.from(habit);
    }

    @Transactional
    public HabitResponse createHabit(UUID userId, CreateHabitRequest req) {
        Habit habit = new Habit();
        habit.setUserId(userId);
        habit.setTitle(req.getTitle());
        habit.setCurrencyPerCompletion(req.getCurrencyPerCompletion() != null ? req.getCurrencyPerCompletion() : 10);
        habit.setStreakTracked(req.getStreakTracked() != null ? req.getStreakTracked() : true);
        habit = habitRepository.save(habit);
        return HabitResponse.from(habit);
    }

    @Transactional
    public HabitResponse updateHabit(UUID id, UUID userId, UpdateHabitRequest req) {
        Habit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));
        if (req.getTitle() != null) habit.setTitle(req.getTitle());
        if (req.getCurrencyPerCompletion() != null) habit.setCurrencyPerCompletion(req.getCurrencyPerCompletion());
        if (req.getStreakTracked() != null) habit.setStreakTracked(req.getStreakTracked());
        habit = habitRepository.save(habit);
        return HabitResponse.from(habit);
    }

    @Transactional
    public void deleteHabit(UUID id, UUID userId) {
        Habit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));
        habit.setDeletedAt(Instant.now());
        habitRepository.save(habit);
    }

    @Transactional
    public HabitCompletionResponse completeHabit(UUID id, UUID userId) {
        Habit habit = habitRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found"));
        ZoneId userZone = ZoneId.of(user.getTimezone());
        LocalDate today = LocalDate.now(userZone);

        if (habitCompletionRepository.findByHabitIdAndCompletionDate(id, today).isPresent()) {
            StreakService.StreakResult existing = habit.isStreakTracked()
                    ? streakService.computeStreak(id, userId, userZone)
                    : new StreakService.StreakResult(0, 0, null);
            return new HabitCompletionResponse(0, toStreakInfo(existing));
        }

        HabitCompletion completion = new HabitCompletion();
        completion.setHabitId(id);
        completion.setCompletedAt(Instant.now());
        completion.setCompletionDate(today);
        habitCompletionRepository.save(completion);

        CityState city = getOrCreateCityState(userId);
        city.setCoins(city.getCoins() + habit.getCurrencyPerCompletion());
        cityStateRepository.save(city);

        StreakService.StreakResult streakResult = habit.isStreakTracked()
                ? streakService.updateStreak(id, userId, userZone)
                : new StreakService.StreakResult(0, 0, today);

        return new HabitCompletionResponse(habit.getCurrencyPerCompletion(), toStreakInfo(streakResult));
    }

    private CityState getOrCreateCityState(UUID userId) {
        return cityStateRepository.findByUserId(userId).orElseGet(() -> {
            CityState s = new CityState();
            s.setUserId(userId);
            s.setCoins(0);
            s.setCityName("My City");
            return cityStateRepository.save(s);
        });
    }

    private static HabitCompletionResponse.StreakInfo toStreakInfo(StreakService.StreakResult r) {
        return new HabitCompletionResponse.StreakInfo(
                r.currentStreakDays,
                r.longestStreakDays,
                r.lastCompletedDate != null ? r.lastCompletedDate.toString() : null
        );
    }
}
