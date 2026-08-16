package com.chessfraud.userservice.user.dto;

public class RegisterResult {
    private final RegisterOutcome outcome;

    public RegisterResult(RegisterOutcome outcome) {
        this.outcome = outcome;
    }

    public boolean isSuccess() {
        return outcome == RegisterOutcome.OK;
    }

    public RegisterOutcome getOutcome() {
        return outcome;
    }
}
