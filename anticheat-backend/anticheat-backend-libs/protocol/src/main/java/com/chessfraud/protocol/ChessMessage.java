package com.chessfraud.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.chessfraud.protocol.payload.ErrorPayload;

import java.util.UUID;

/**
 * Wire envelope for every message exchanged between the gateway and its clients.
 * The {@code payload} shape is resolved at deserialization time by {@link PayloadRegistry}
 * based on {@code type}; see {@link MessageType} for the full catalog.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChessMessage(
    MessageType type,
    String messageId,
    Object payload
) {
    public ChessMessage(MessageType type, Object payload) {
        this(type, UUID.randomUUID().toString(), payload);
    }

    public static ChessMessage error(String code, String message) {
        return new ChessMessage(MessageType.ERROR, new ErrorPayload(code, message));
    }

    public static ChessMessage error(String code, String message, String correlationId) {
        return new ChessMessage(MessageType.ERROR, correlationId, new ErrorPayload(code, message));
    }
}
