package com.chessfraud.gameservice.game.dto;

/**
 * Represents the outcome of a save-game operation.
 */
public class SaveGameResult {

    private final boolean success;

    public SaveGameResult(boolean success) {
        this.success = success;
    }

    /** @return {@code true} if the game record was persisted successfully */
    public boolean isSuccess() {
        return success;
    }
}
