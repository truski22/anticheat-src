package grpc;

import com.chessfraud.grpc.game.*;
import io.grpc.stub.StreamObserver;
import model.Game;
import service.GamePersistenceService;
import service.GamesResult;
import service.SaveGameResult;

public class GameServiceGrpcImpl extends GameServiceGrpc.GameServiceImplBase {
    private final GamePersistenceService gamePersistenceService;

    public GameServiceGrpcImpl(GamePersistenceService gamePersistenceService) {
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
