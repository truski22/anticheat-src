package com.chessfraud.gateway.auth.dto;

public record RegisterRequestBody(String user, String email, String password) {
}
