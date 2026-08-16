package com.chessfraud.protocol.payload;

public record ChangePasswordEmailPayload(String email, String password) {
    public ChangePasswordEmailPayload {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
