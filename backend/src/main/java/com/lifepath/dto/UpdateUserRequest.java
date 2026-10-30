package com.lifepath.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @Size(max = 100)
    private String displayName;

    private String timezone;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
