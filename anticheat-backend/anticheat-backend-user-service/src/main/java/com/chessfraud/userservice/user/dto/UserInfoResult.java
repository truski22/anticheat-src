package com.chessfraud.userservice.user.dto;

public class UserInfoResult {
    private final String email;
    private final int totalGames;
    private final int cheatedGames;
    private final int fairGames;

    public UserInfoResult(String email, int totalGames, int cheatedGames, int fairGames) {
        this.email = email;
        this.totalGames = totalGames;
        this.cheatedGames = cheatedGames;
        this.fairGames = fairGames;
    }

    public String getEmail() { return email; }
    public int getTotalGames() { return totalGames; }
    public int getCheatedGames() { return cheatedGames; }
    public int getFairGames() { return fairGames; }
}
