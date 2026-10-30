package com.lifepath.dto;

import com.lifepath.model.enums.TaskType;
import com.lifepath.model.enums.UnblockTrigger;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class BlockTaskRequest {

    @NotNull
    private TaskType taskType;

    @NotNull
    private UUID taskId;

    private String reason;

    @NotNull
    private UnblockTrigger unblockTrigger;

    private Instant unblockAt;

    private String unblockLocation;

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UnblockTrigger getUnblockTrigger() { return unblockTrigger; }
    public void setUnblockTrigger(UnblockTrigger unblockTrigger) { this.unblockTrigger = unblockTrigger; }
    public Instant getUnblockAt() { return unblockAt; }
    public void setUnblockAt(Instant unblockAt) { this.unblockAt = unblockAt; }
    public String getUnblockLocation() { return unblockLocation; }
    public void setUnblockLocation(String unblockLocation) { this.unblockLocation = unblockLocation; }
}
