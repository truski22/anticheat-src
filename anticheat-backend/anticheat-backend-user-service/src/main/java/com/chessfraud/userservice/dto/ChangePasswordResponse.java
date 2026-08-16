package com.chessfraud.userservice.dto;

public class ChangePasswordResponse {
    private final boolean success;

    public ChangePasswordResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
