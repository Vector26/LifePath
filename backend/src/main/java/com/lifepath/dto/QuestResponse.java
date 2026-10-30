package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifepath.model.Quest;
import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.PriorityLevel;
import com.lifepath.model.enums.QuestStatus;
import com.lifepath.model.enums.TimeOfDay;

import java.time.Instant;
import java.util.UUID;

public class QuestResponse {

    private UUID id;
    private String title;
    private String notes;

    @JsonProperty("priority_level")
    private PriorityLevel priorityLevel;

    @JsonProperty("parent_id")
    private UUID parentId;

    private QuestStatus status;

    @JsonProperty("time_of_day")
    private TimeOfDay timeOfDay;

    @JsonProperty("context_tag")
    private ContextTag contextTag;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    public static QuestResponse from(Quest q) {
        QuestResponse r = new QuestResponse();
        r.id = q.getId();
        r.title = q.getTitle();
        r.notes = q.getNotes();
        r.priorityLevel = q.getPriorityLevel();
        r.parentId = q.getParentId();
        r.status = q.getStatus();
        r.timeOfDay = q.getTimeOfDay();
        r.contextTag = q.getContextTag();
        r.createdAt = q.getCreatedAt();
        r.updatedAt = q.getUpdatedAt();
        r.completedAt = q.getCompletedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public UUID getParentId() { return parentId; }
    public QuestStatus getStatus() { return status; }
    public TimeOfDay getTimeOfDay() { return timeOfDay; }
    public ContextTag getContextTag() { return contextTag; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
