package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentObjectiveResponse {

    public enum ObjectiveType { quest, habit, calendar_event }

    private ObjectiveType type;
    private Object payload;
    private List<String> reasons;

    public CurrentObjectiveResponse(ObjectiveType type, Object payload, List<String> reasons) {
        this.type = type;
        this.payload = payload;
        this.reasons = reasons;
    }

    public ObjectiveType getType() { return type; }
    public Object getPayload() { return payload; }
    public List<String> getReasons() { return reasons; }
}
