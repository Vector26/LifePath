package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentQuestResponse {

    private QuestResponse quest;
    private List<String> reasons;

    public CurrentQuestResponse(QuestResponse quest, List<String> reasons) {
        this.quest = quest;
        this.reasons = reasons;
    }

    public QuestResponse getQuest() { return quest; }
    public List<String> getReasons() { return reasons; }
}
