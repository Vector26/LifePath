package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifepath.model.CalendarEvent;

import java.time.Instant;
import java.util.UUID;

public class CalendarEventResponse {

    private UUID id;
    private String title;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("start_time")
    private Instant startTime;

    @JsonProperty("end_time")
    private Instant endTime;

    private boolean completed;

    public static CalendarEventResponse from(CalendarEvent e) {
        CalendarEventResponse r = new CalendarEventResponse();
        r.id = e.getId();
        r.title = e.getTitle();
        r.externalId = e.getExternalId();
        r.startTime = e.getStartTime();
        r.endTime = e.getEndTime();
        r.completed = e.isCompleted();
        return r;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getExternalId() { return externalId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public boolean isCompleted() { return completed; }
}
