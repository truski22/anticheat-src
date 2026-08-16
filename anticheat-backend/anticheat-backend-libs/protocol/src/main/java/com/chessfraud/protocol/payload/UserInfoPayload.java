package com.chessfraud.protocol.payload;

public record UserInfoPayload(String email, int totalGames, int cheatGames, int legalGames) {}
