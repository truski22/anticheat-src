package mqtt;

import java.util.ArrayList;
import java.util.List;

public class Games extends Common{
    List<Game> games = new ArrayList<>();

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    public void addGame(Game game){
        this.games.add(game);
    }
}
