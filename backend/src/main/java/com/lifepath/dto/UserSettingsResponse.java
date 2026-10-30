package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSettingsResponse {

    @JsonProperty("notifications_enabled")
    private boolean notificationsEnabled = true;

    @JsonProperty("haptic_feedback")
    private boolean hapticFeedback = true;

    private String theme = "system";

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public boolean isHapticFeedback() { return hapticFeedback; }
    public void setHapticFeedback(boolean hapticFeedback) { this.hapticFeedback = hapticFeedback; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
