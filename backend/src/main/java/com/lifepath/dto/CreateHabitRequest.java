package com.lifepath.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateHabitRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String title;

    private Integer currencyPerCompletion;

    private Boolean streakTracked;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getCurrencyPerCompletion() { return currencyPerCompletion; }
    public void setCurrencyPerCompletion(Integer currencyPerCompletion) { this.currencyPerCompletion = currencyPerCompletion; }
    public Boolean getStreakTracked() { return streakTracked; }
    public void setStreakTracked(Boolean streakTracked) { this.streakTracked = streakTracked; }
}
