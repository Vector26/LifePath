package com.lifepath.exception;

public class CalendarApiDisabledException extends RuntimeException {
    private final String activationUrl;

    public CalendarApiDisabledException(String message, String activationUrl) {
        super(message);
        this.activationUrl = activationUrl;
    }

    public String getActivationUrl() {
        return activationUrl;
    }
}
