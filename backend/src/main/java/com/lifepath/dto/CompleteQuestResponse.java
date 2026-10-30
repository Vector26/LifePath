package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompleteQuestResponse {

    private QuestResponse quest;

    @JsonProperty("xp_earned")
    private int xpEarned;

    public CompleteQuestResponse(QuestResponse quest, int xpEarned) {
        this.quest = quest;
        this.xpEarned = xpEarned;
    }

    public QuestResponse getQuest() { return quest; }
    public int getXpEarned() { return xpEarned; }
}
