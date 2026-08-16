package com.chessfraud.gameservice.grpc;

import com.chessfraud.grpc.game.GameRecord;
import com.chessfraud.grpc.game.GameServiceGrpc;
import com.chessfraud.grpc.game.GetGamesRequest;
import com.chessfraud.grpc.game.GetGamesResponse;
import com.chessfraud.grpc.game.SaveGameRequest;
import com.chessfraud.grpc.game.SaveGameResponse;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the actual contract consumed by the Gateway (GameService.GetGames/SaveGame).
 * Runs against an in-process gRPC server (grpc.server.in-process-name in
 * src/test/resources/application.properties) and in-memory H2 — no network, no Postgres.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class GameGrpcServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ManagedChannel channel;
    private GameServiceGrpc.GameServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        channel = InProcessChannelBuilder.forName("test-game-service").directExecutor().build();
        stub = GameServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void saveGameThenGetGamesReturnsIt() {
        SaveGameResponse save = stub.saveGame(SaveGameRequest.newBuilder()
                .setUser("alice")
                .setMoves("e2e4 e7e5")
                .setLegal(true)
                .build());
        assertThat(save.getSuccess()).isTrue();

        GetGamesResponse games = stub.getGames(GetGamesRequest.newBuilder().setUser("alice").build());
        assertThat(games.getGamesList()).hasSize(1);
        GameRecord record = games.getGames(0);
        assertThat(record.getMoves()).isEqualTo("e2e4 e7e5");
        assertThat(record.getLegal()).isTrue();
    }

    @Test
    void saveGameUpdatesUserCountersAccordingToLegality() {
        jdbcTemplate.update("INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
                "bob", "bob@example.com", "hash");

        stub.saveGame(SaveGameRequest.newBuilder().setUser("bob").setMoves("d2d4").setLegal(false).build());

        Integer totalGames = jdbcTemplate.queryForObject("SELECT total_games FROM users WHERE name = ?", Integer.class, "bob");
        Integer cheatedGames = jdbcTemplate.queryForObject("SELECT cheated_games FROM users WHERE name = ?", Integer.class, "bob");
        Integer fairGames = jdbcTemplate.queryForObject("SELECT fair_games FROM users WHERE name = ?", Integer.class, "bob");
        assertThat(totalGames).isEqualTo(1);
        assertThat(cheatedGames).isEqualTo(1);
        assertThat(fairGames).isEqualTo(0);
    }

    @Test
    void saveGameForUnknownUserStillSucceeds() {
        // Matches the original hand-rolled behaviour: no existence check before the
        // counter UPDATE, so an unknown user simply means 0 rows affected there.
        SaveGameResponse save = stub.saveGame(SaveGameRequest.newBuilder()
                .setUser("ghost")
                .setMoves("a2a3")
                .setLegal(true)
                .build());
        assertThat(save.getSuccess()).isTrue();
    }

    @Test
    void getGamesForUnknownUserReturnsEmptyList() {
        GetGamesResponse games = stub.getGames(GetGamesRequest.newBuilder().setUser("nobody").build());
        assertThat(games.getGamesList()).isEmpty();
    }
}
