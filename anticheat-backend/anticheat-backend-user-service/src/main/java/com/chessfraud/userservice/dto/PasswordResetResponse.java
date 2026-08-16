package com.chessfraud.userservice.dto;

public class PasswordResetResponse {
    private final boolean sent;
    private final String code;

    public PasswordResetResponse(boolean sent, String code) {
        this.sent = sent;
        this.code = code;
    }

    public boolean isSent() {
        return sent;
    }

    public String getCode() {
        return code;
    }
}
