package com.lifepath.exception;

public class UnprocessableEntityException extends RuntimeException {

    private final String errorCode;

    public UnprocessableEntityException(String message) {
        super(message);
        this.errorCode = "UNPROCESSABLE_ENTITY";
    }

    public UnprocessableEntityException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
