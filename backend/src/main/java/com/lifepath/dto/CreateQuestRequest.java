package com.lifepath.dto;

import com.lifepath.model.enums.ContextTag;
import com.lifepath.model.enums.PriorityLevel;
import com.lifepath.model.enums.TimeOfDay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateQuestRequest {

    @NotBlank
    @Size(min = 1, max = 500)
    private String title;

    @Size(max = 5000)
    private String notes;

    @jakarta.validation.constraints.NotNull
    private PriorityLevel priorityLevel;

    private UUID parentId;

    private TimeOfDay timeOfDay = TimeOfDay.any;

    private ContextTag contextTag;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public TimeOfDay getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(TimeOfDay timeOfDay) { this.timeOfDay = timeOfDay != null ? timeOfDay : TimeOfDay.any; }
    public ContextTag getContextTag() { return contextTag; }
    public void setContextTag(ContextTag contextTag) { this.contextTag = contextTag; }
}
