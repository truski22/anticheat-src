package com.chessfraud.userservice.dto;

public class RegisterResponse {
    private final boolean success;
    private final String message;

    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
