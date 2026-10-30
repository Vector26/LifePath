package com.lifepath.repository;

import com.lifepath.model.BlockedTask;
import com.lifepath.model.enums.TaskType;
import com.lifepath.model.enums.UnblockTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedTaskRepository extends JpaRepository<BlockedTask, UUID> {

    List<BlockedTask> findByUserIdAndTaskTypeAndUnblockedAtIsNull(UUID userId, TaskType taskType);

    Optional<BlockedTask> findByUserIdAndTaskTypeAndTaskIdAndUnblockedAtIsNull(UUID userId, TaskType taskType, UUID taskId);

    Optional<BlockedTask> findTopByUserIdAndUnblockTriggerAndUnblockedAtIsNullOrderByCreatedAtAsc(UUID userId, UnblockTrigger trigger);
}
