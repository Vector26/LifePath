package com.lifepath.model;

import com.lifepath.model.enums.TaskType;
import com.lifepath.model.enums.UnblockTrigger;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocked_tasks")
public class BlockedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "unblock_trigger", nullable = false, length = 20)
    private UnblockTrigger unblockTrigger;

    @Column(name = "unblock_at")
    private Instant unblockAt;

    @Column(name = "unblock_location", length = 255)
    private String unblockLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "unblocked_at")
    private Instant unblockedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUnblockedAt() { return unblockedAt; }
    public void setUnblockedAt(Instant unblockedAt) { this.unblockedAt = unblockedAt; }
}
