package com.chessfraud.gameservice.game.service;

import com.chessfraud.gameservice.game.dto.GamesResult;
import com.chessfraud.gameservice.game.dto.SaveGameResult;
import com.chessfraud.gameservice.game.model.Game;
import com.chessfraud.gameservice.game.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

/**
 * Provides persistence operations for chess games. Saving a game also updates the
 * requesting user's aggregate counters (users.total_games/fair_games/cheated_games) —
 * a table owned by user-service, but shared in the same PostgreSQL instance
 * (anticheat-backend-infra/docker/init.sql).
 */
@Service
public class GamePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GamePersistenceService.class);

    private static final String LEGAL_COUNTER_UPDATE =
            "UPDATE users SET total_games = total_games + 1, fair_games = fair_games + 1 WHERE name = ?";
    private static final String CHEAT_COUNTER_UPDATE =
            "UPDATE users SET total_games = total_games + 1, cheated_games = cheated_games + 1 WHERE name = ?";

    private final GameRepository gameRepository;
    private final JdbcTemplate jdbcTemplate;

    public GamePersistenceService(GameRepository gameRepository, JdbcTemplate jdbcTemplate) {
        this.gameRepository = gameRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists a completed game record for the given user and updates their aggregate
     * counters, in a single transaction.
     *
     * @param username account name of the player
     * @param moves    serialised move list (e.g. PGN or move-integer string)
     * @param legal    {@code true} if the engine considers the game fair, {@code false} if cheating was detected
     * @return {@link SaveGameResult} indicating whether the insert succeeded
     */
    @Transactional
    public SaveGameResult saveGame(String username, String moves, boolean legal) {
        try {
            gameRepository.save(new Game(username, moves, legal));
            jdbcTemplate.update(legal ? LEGAL_COUNTER_UPDATE : CHEAT_COUNTER_UPDATE, username);
            return new SaveGameResult(true);
        } catch (DataAccessException e) {
            log.error("[GAME-SERVICE] DB error in saveGame for user '{}': {}", username, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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
        try {
            return new GamesResult(gameRepository.findByUsername(username));
        } catch (DataAccessException e) {
            log.error("[GAME-SERVICE] DB error in getGames for user '{}': {}", username, e.getMessage(), e);
            return new GamesResult(List.of());
        }
    }
}
