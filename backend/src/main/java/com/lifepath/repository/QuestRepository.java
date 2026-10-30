package com.lifepath.repository;

import com.lifepath.model.Quest;
import com.lifepath.model.enums.QuestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<Quest, UUID> {

    long countByUserIdAndStatusAndCompletedAtBetween(UUID userId, QuestStatus status, Instant start, Instant end);

    List<Quest> findByUserIdAndStatus(UUID userId, QuestStatus status);

    List<Quest> findByUserId(UUID userId, Pageable pageable);

    Optional<Quest> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByParentId(UUID parentId);

    long countByParentId(UUID parentId);
}
