package com.chessfraud.gameservice.grpc;

import com.chessfraud.gameservice.dto.game.GamesResult;
import com.chessfraud.gameservice.dto.game.SaveGameResult;
import com.chessfraud.gameservice.model.game.Game;
import com.chessfraud.gameservice.service.game.GamePersistenceService;
import com.chessfraud.grpc.game.GameRecord;
import com.chessfraud.grpc.game.GameServiceGrpc;
import com.chessfraud.grpc.game.GetGamesRequest;
import com.chessfraud.grpc.game.GetGamesResponse;
import com.chessfraud.grpc.game.SaveGameRequest;
import com.chessfraud.grpc.game.SaveGameResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Implements the contract from anticheat-backend-libs/grpc-api/game_service.proto
 * (same default host/port "game-service:9091" expected by
 * anticheat-backend-gateway/grpc/ServiceClients.java).
 */
@GrpcService
public class GameGrpcService extends GameServiceGrpc.GameServiceImplBase {

    private final GamePersistenceService gamePersistenceService;

    public GameGrpcService(GamePersistenceService gamePersistenceService) {
        this.gamePersistenceService = gamePersistenceService;
    }

    @Override
    public void getGames(GetGamesRequest request, StreamObserver<GetGamesResponse> responseObserver) {
        GamesResult result = gamePersistenceService.getGames(request.getUser());
        GetGamesResponse.Builder responseBuilder = GetGamesResponse.newBuilder();

        for (Game game : result.getGames()) {
            responseBuilder.addGames(GameRecord.newBuilder()
                    .setMoves(game.getMoves())
                    .setLegal(game.isLegal())
                    .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void saveGame(SaveGameRequest request, StreamObserver<SaveGameResponse> responseObserver) {
        SaveGameResult result = gamePersistenceService.saveGame(
                request.getUser(),
                request.getMoves(),
                request.getLegal());

        SaveGameResponse response = SaveGameResponse.newBuilder()
                .setSuccess(result.isSuccess())
                .setMessage(result.isSuccess() ? "Game saved successfully" : "Failed to save game")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
