package service;

import model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides persistence operations for chess games.
 * All methods handle their own {@link SQLException} internally and communicate
 * results via safe return objects.
 */
public class GamePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GamePersistenceService.class);

    private final Database database;

    /**
     * @param database shared database connection pool
     */
    public GamePersistenceService(Database database) {
        this.database = database;
    }

    /**
     * Persists a completed game record for the given user.
     *
     * @param username account name of the player
     * @param moves    serialised move list (e.g. PGN or move-integer string)
     * @param legal    {@code true} if the engine considers the game fair, {@code false} if cheating was detected
     * @return {@link SaveGameResult} indicating whether the insert succeeded
     */
    public SaveGameResult saveGame(String username, String moves, boolean legal) {
        String insertQuery = "INSERT INTO games (username, moves, legal) VALUES (?, ?, ?)";
        String updateQuery = legal
                ? "UPDATE users SET total_games = total_games + 1, fair_games = fair_games + 1 WHERE name = ?"
                : "UPDATE users SET total_games = total_games + 1, cheated_games = cheated_games + 1 WHERE name = ?";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insertPst = connection.prepareStatement(insertQuery)) {
                insertPst.setString(1, username);
                insertPst.setString(2, moves);
                insertPst.setBoolean(3, legal);
                insertPst.executeUpdate();
            }
            try (PreparedStatement updatePst = connection.prepareStatement(updateQuery)) {
                updatePst.setString(1, username);
                updatePst.executeUpdate();
            }
            connection.commit();
            return new SaveGameResult(true);
        } catch (SQLException e) {
            log.error("[GAME-SERVICE] DB error in saveGame for user '{}': {}", username, e.getMessage(), e);
            return new SaveGameResult(false);
        }
    }

    /**
     * Retrieves all games recorded for a user.
     *
     * @param username account name of the player
     * @return {@link GamesResult} containing the list of games (may be empty, never {@code null})
     */
    public GamesResult getGames(String username) {
        String query = "SELECT moves, legal FROM games WHERE username = ?";
        List<Game> games = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Game game = new Game();
                    game.setMoves(rs.getString("moves"));
                    game.setLegal(rs.getBoolean("legal"));
                    games.add(game);
                }
            }
        } catch (SQLException e) {
            log.error("[GAME-SERVICE] DB error in getGames for user '{}': {}", username, e.getMessage(), e);
        }
        return new GamesResult(games);
    }
}
