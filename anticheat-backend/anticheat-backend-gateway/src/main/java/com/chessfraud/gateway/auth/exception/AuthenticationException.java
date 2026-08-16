package com.chessfraud.gateway.auth.exception;

/** Missing, malformed, or invalid/expired JWT on a route that requires one. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
