package com.chessfraud.gateway.auth.exception;

/** Too many login/register attempts from the same client within the current window. */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
