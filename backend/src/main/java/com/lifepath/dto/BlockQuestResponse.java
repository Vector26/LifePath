package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockQuestResponse {

    private QuestResponse quest;

    @JsonProperty("next_quest")
    private QuestResponse nextQuest;

    @JsonProperty("xp_earned")
    private int xpEarned;

    public BlockQuestResponse(QuestResponse quest, QuestResponse nextQuest, int xpEarned) {
        this.quest = quest;
        this.nextQuest = nextQuest;
        this.xpEarned = xpEarned;
    }

    public QuestResponse getQuest() { return quest; }
    public QuestResponse getNextQuest() { return nextQuest; }
    public int getXpEarned() { return xpEarned; }
}
