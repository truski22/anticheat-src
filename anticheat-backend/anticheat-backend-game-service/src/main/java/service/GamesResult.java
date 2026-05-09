package service;

import mqtt.Game;

import java.util.List;

/**
 * Carries the list of games retrieved from the database for a given user.
 */
public class GamesResult {

    private final List<Game> games;

    public GamesResult(List<Game> games) {
        this.games = games;
    }

    /** @return list of {@link Game} objects belonging to the requested user */
    public List<Game> getGames() { return games; }
}
