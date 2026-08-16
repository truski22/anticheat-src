package com.chessfraud.userservice.user.dto;

public class LoginResult {
    private final LoginOutcome outcome;

    public LoginResult(LoginOutcome outcome) {
        this.outcome = outcome;
    }

    public boolean isSuccess() {
        return outcome == LoginOutcome.OK;
    }

    public LoginOutcome getOutcome() {
        return outcome;
    }
}
