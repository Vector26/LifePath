package com.lifepath.repository;

import com.lifepath.model.CityState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CityStateRepository extends JpaRepository<CityState, UUID> {

    Optional<CityState> findByUserId(UUID userId);
}
