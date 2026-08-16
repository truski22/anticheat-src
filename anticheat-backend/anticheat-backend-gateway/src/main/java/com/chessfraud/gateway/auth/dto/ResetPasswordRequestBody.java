package com.chessfraud.gateway.auth.dto;

public record ResetPasswordRequestBody(String email, String code, String password) {
}
