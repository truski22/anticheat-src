package com.chessfraud.protocol;

/**
 * Every message type carried inside a {@link ChessMessage} envelope.
 * Adding a type requires a matching entry in {@link PayloadRegistry} unless the
 * message carries no payload.
 */
public enum MessageType {
    // Client -> Gateway
    LOGIN, REGISTER, USER_INFO_REQUEST, GAMES_REQUEST,
    ANALYZE_GAME, SAVE_GAME,
    CHANGE_PASSWORD, CHANGE_PASSWORD_EMAIL, SEND_EMAIL_CHANGE_PASSWORD,

    // Gateway -> Client
    LOGIN_RESPONSE, REGISTER_RESPONSE, USER_INFO, GAMES,
    ANALYZE_RESULT, SAVE_GAME_RESPONSE,
    CHANGE_PASSWORD_RESPONSE, CHANGE_PASSWORD_EMAIL_RESPONSE,

    // Future game types
    MOVE, RESIGN, CHAT, FRAUD_ALERT,

    // System
    ERROR
}
