package com.lifepath.repository;

import com.lifepath.model.UserBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBuildingRepository extends JpaRepository<UserBuilding, UUID> {
    List<UserBuilding> findByUserId(UUID userId);
    Optional<UserBuilding> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndGridXAndGridY(UUID userId, int gridX, int gridY);
    List<UserBuilding> findByUserIdAndDecayedAtIsNullAndExpiresAtBefore(UUID userId, Instant now);
    List<UserBuilding> findByUserIdAndDecayedAtIsNotNullAndRemoveAfterAtBefore(UUID userId, Instant now);
    List<UserBuilding> findByDecayedAtIsNullAndExpiresAtBefore(Instant now);
    List<UserBuilding> findByDecayedAtIsNotNullAndRemoveAfterAtBefore(Instant now);
}
