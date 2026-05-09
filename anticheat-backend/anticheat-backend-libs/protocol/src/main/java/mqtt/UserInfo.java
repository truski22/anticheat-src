package mqtt;

public class UserInfo extends Common{
    private String user;
    private String email;
    private int totalGames;
    private int cheatGames;
    private int legalGames;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getCheatGames() {
        return cheatGames;
    }

    public void setCheatGames(int cheatGames) {
        this.cheatGames = cheatGames;
    }

    public int getLegalGames() {
        return legalGames;
    }

    public void setLegalGames(int legalGames) {
        this.legalGames = legalGames;
    }
}
