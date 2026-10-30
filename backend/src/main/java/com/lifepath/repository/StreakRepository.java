package com.lifepath.repository;

import com.lifepath.model.Streak;
import com.lifepath.model.StreakId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreakRepository extends JpaRepository<Streak, StreakId> {

    List<Streak> findByUserId(UUID userId);

    Optional<Streak> findByHabitIdAndUserId(UUID habitId, UUID userId);
}
