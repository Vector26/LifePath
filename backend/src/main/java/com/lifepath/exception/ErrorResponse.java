package com.lifepath.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Error error;

    public ErrorResponse(String code, String message) {
        this.error = new Error(code, message, null);
    }

    public ErrorResponse(String code, String message, List<Detail> details) {
        this.error = new Error(code, message, details);
    }

    public Error getError() {
        return error;
    }

    public static class Error {
        private String code;
        private String message;
        private List<Detail> details;

        public Error(String code, String message, List<Detail> details) {
            this.code = code;
            this.message = message;
            this.details = details != null ? details : new ArrayList<>();
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public List<Detail> getDetails() { return details; }
    }

    public static class Detail {
        private String field;
        private String reason;

        public Detail(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public String getField() { return field; }
        public String getReason() { return reason; }
    }
}
