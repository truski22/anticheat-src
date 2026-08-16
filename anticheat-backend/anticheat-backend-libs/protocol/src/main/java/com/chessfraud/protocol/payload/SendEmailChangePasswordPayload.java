package com.chessfraud.protocol.payload;

public record SendEmailChangePasswordPayload(String email) {
    public SendEmailChangePasswordPayload {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
    }
}
