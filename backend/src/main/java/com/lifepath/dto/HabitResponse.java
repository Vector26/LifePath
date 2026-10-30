package com.lifepath.dto;

import com.lifepath.model.Habit;

import java.util.UUID;

public class HabitResponse {

    private UUID id;
    private String title;
    private int currencyPerCompletion;
    private boolean streakTracked;

    public static HabitResponse from(Habit h) {
        HabitResponse r = new HabitResponse();
        r.id = h.getId();
        r.title = h.getTitle();
        r.currencyPerCompletion = h.getCurrencyPerCompletion();
        r.streakTracked = h.isStreakTracked();
        return r;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public int getCurrencyPerCompletion() { return currencyPerCompletion; }
    public boolean isStreakTracked() { return streakTracked; }
}
