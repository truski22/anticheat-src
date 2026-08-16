package com.chessfraud.userservice.dto.user;

public class RegisterResult {
    private final String response;

    public RegisterResult(String response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return "OK".equals(response);
    }

    public String getResponse() {
        return response;
    }
}
