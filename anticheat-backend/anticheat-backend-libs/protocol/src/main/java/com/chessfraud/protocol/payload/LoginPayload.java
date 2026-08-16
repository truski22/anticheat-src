package com.chessfraud.protocol.payload;

public record LoginPayload(String password) {
    public LoginPayload {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
