package com.lifepath.repository;

import com.lifepath.model.HabitCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    Optional<HabitCompletion> findByHabitIdAndCompletionDate(UUID habitId, LocalDate completionDate);

    List<HabitCompletion> findByHabitIdOrderByCompletedAtDesc(UUID habitId);

    long countByHabitIdInAndCompletionDate(Set<UUID> habitIds, LocalDate completionDate);
}
