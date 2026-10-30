package com.lifepath.service;

import com.lifepath.dto.BlockTaskRequest;
import com.lifepath.exception.UnprocessableEntityException;
import com.lifepath.model.BlockedTask;
import com.lifepath.model.enums.TaskType;
import com.lifepath.model.enums.UnblockTrigger;
import com.lifepath.repository.BlockedTaskRepository;
import com.lifepath.repository.CalendarEventRepository;
import com.lifepath.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskBlockingService {

    private final BlockedTaskRepository blockedTaskRepository;
    private final HabitRepository habitRepository;
    private final CalendarEventRepository calendarEventRepository;

    public TaskBlockingService(BlockedTaskRepository blockedTaskRepository,
                               HabitRepository habitRepository,
                               CalendarEventRepository calendarEventRepository) {
        this.blockedTaskRepository = blockedTaskRepository;
        this.habitRepository = habitRepository;
        this.calendarEventRepository = calendarEventRepository;
    }

    @Transactional
    public void blockTask(UUID userId, BlockTaskRequest req) {
        validateRequest(req);
        validateOwnershipAndState(userId, req.getTaskType(), req.getTaskId());

        if (blockedTaskRepository
                .findByUserIdAndTaskTypeAndTaskIdAndUnblockedAtIsNull(userId, req.getTaskType(), req.getTaskId())
                .isPresent()) {
            throw new UnprocessableEntityException("TASK_ALREADY_BLOCKED", "Task is already blocked");
        }

        BlockedTask blockedTask = new BlockedTask();
        blockedTask.setUserId(userId);
        blockedTask.setTaskType(req.getTaskType());
        blockedTask.setTaskId(req.getTaskId());
        blockedTask.setReason(req.getReason());
        blockedTask.setUnblockTrigger(req.getUnblockTrigger());
        blockedTask.setUnblockAt(req.getUnblockAt());
        blockedTask.setUnblockLocation(req.getUnblockLocation());
        blockedTaskRepository.save(blockedTask);
    }

    @Transactional
    public Set<UUID> getActiveBlockedTaskIds(UUID userId, TaskType taskType, Instant now) {
        List<BlockedTask> active = blockedTaskRepository.findByUserIdAndTaskTypeAndUnblockedAtIsNull(userId, taskType);
        Set<UUID> blockedIds = new HashSet<>();
        for (BlockedTask task : active) {
            if (task.getUnblockTrigger() == UnblockTrigger.time
                    && task.getUnblockAt() != null
                    && !task.getUnblockAt().isAfter(now)) {
                task.setUnblockedAt(now);
                blockedTaskRepository.save(task);
                continue;
            }
            blockedIds.add(task.getTaskId());
        }
        return blockedIds;
    }

    private void validateRequest(BlockTaskRequest req) {
        if (req.getUnblockTrigger() == UnblockTrigger.time && req.getUnblockAt() == null) {
            throw new UnprocessableEntityException("MISSING_UNBLOCK_AT", "unblockAt required when unblockTrigger is time");
        }
        if (req.getUnblockTrigger() == UnblockTrigger.location
                && (req.getUnblockLocation() == null || req.getUnblockLocation().trim().isEmpty())) {
            throw new UnprocessableEntityException("MISSING_UNBLOCK_LOCATION", "unblockLocation required when unblockTrigger is location");
        }
        if (req.getTaskType() == TaskType.quest) {
            throw new UnprocessableEntityException("USE_QUEST_BLOCK", "Use /quests/{id}/block for quests");
        }
    }

    private void validateOwnershipAndState(UUID userId, TaskType taskType, UUID taskId) {
        switch (taskType) {
            case habit -> habitRepository.findByIdAndUserId(taskId, userId)
                    .filter(h -> h.getDeletedAt() == null)
                    .orElseThrow(() -> new java.util.NoSuchElementException("Habit not found"));
            case calendar_event -> calendarEventRepository.findByIdAndUserId(taskId, userId)
                    .filter(e -> !e.isCompleted())
                    .orElseThrow(() -> new java.util.NoSuchElementException("Calendar event not found or already completed"));
            case quest -> {
                // quests blocked via QuestService
            }
        }
    }
}
