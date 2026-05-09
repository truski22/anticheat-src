package service;

/**
 * Carries the profile information fetched from the database for a single user.
 */
public class UserInfoResult {

    private final String email;
    private final int totalGames;
    private final int cheatedGames;
    private final int fairGames;

    public UserInfoResult(String email, int totalGames, int cheatedGames, int fairGames) {
        this.email        = email;
        this.totalGames   = totalGames;
        this.cheatedGames = cheatedGames;
        this.fairGames    = fairGames;
    }

    /** @return user's e-mail address */
    public String getEmail() { return email; }

    /** @return total games played */
    public int getTotalGames() { return totalGames; }

    /** @return number of games flagged as cheated */
    public int getCheatedGames() { return cheatedGames; }

    /** @return number of games considered fair */
    public int getFairGames() { return fairGames; }
}
