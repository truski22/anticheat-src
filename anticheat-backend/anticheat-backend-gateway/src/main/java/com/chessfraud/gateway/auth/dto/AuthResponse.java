package com.chessfraud.gateway.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(boolean success, String message, String token) {
    public AuthResponse(boolean success, String message) {
        this(success, message, null);
    }
}
