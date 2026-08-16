package com.chessfraud.userservice.dto.user;

public class LoginResult {
    private final String response;

    public LoginResult(String response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return "OK".equals(response);
    }

    public String getResponse() {
        return response;
    }
}
