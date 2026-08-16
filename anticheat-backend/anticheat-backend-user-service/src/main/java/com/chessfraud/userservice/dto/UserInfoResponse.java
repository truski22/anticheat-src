package com.chessfraud.userservice.dto;

public class UserInfoResponse {
    private final String user;
    private final String email;
    private final int totalGames;
    private final int cheatGames;
    private final int legalGames;

    public UserInfoResponse(String user, String email, int totalGames, int cheatGames, int legalGames) {
        this.user = user;
        this.email = email;
        this.totalGames = totalGames;
        this.cheatGames = cheatGames;
        this.legalGames = legalGames;
    }

    public String getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getCheatGames() {
        return cheatGames;
    }

    public int getLegalGames() {
        return legalGames;
    }
}
