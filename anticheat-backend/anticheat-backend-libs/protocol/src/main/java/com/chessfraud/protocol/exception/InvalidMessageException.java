package com.chessfraud.protocol.exception;

/**
 * Thrown when an inbound message fails structural or semantic validation
 * (unknown type, missing/oversized/malformed payload, etc.).
 * {@link #getCode()} is a stable machine-readable reason sent back to the client
 * as part of a {@code ChessMessage.error(...)} response.
 */
public class InvalidMessageException extends Exception {
    private final String code;

    public InvalidMessageException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
