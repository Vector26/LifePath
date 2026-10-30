package com.lifepath.repository;

import com.lifepath.model.BlockedQuest;
import com.lifepath.model.enums.UnblockTrigger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedQuestRepository extends JpaRepository<BlockedQuest, UUID> {

    Optional<BlockedQuest> findByQuestIdAndUnblockedAtIsNull(UUID questId);

    List<BlockedQuest> findByUnblockTriggerAndUnblockedAtIsNullAndUnblockAtBefore(UnblockTrigger trigger, Instant before);

    Optional<BlockedQuest> findTopByQuestIdAndUnblockedAtIsNotNullOrderByUnblockedAtDesc(UUID questId);

    @Query("SELECT bq FROM BlockedQuest bq JOIN Quest q ON bq.questId = q.id " +
           "WHERE q.userId = :userId AND bq.unblockTrigger = :trigger AND bq.unblockedAt IS NULL " +
           "ORDER BY bq.createdAt ASC")
    List<BlockedQuest> findManualBlocksForUser(@Param("userId") UUID userId,
                                               @Param("trigger") UnblockTrigger trigger,
                                               Pageable pageable);
}
