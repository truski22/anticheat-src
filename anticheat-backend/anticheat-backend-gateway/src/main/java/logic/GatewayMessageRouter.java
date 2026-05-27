package logic;

import auth.AuthController;
import auth.JwtService;
import com.chessfraud.grpc.analysis.AnalyzeGameRequest;
import com.chessfraud.grpc.analysis.AnalyzeGameResponse;
import com.chessfraud.grpc.game.GetGamesRequest;
import com.chessfraud.grpc.game.GetGamesResponse;
import com.chessfraud.grpc.game.SaveGameRequest;
import com.chessfraud.grpc.game.SaveGameResponse;
import com.chessfraud.grpc.user.ChangePasswordRequest;
import com.chessfraud.grpc.user.ChangePasswordResponse;
import com.chessfraud.grpc.user.UserInfoRequest;
import com.chessfraud.grpc.user.UserInfoResponse;
import grpc.ServiceClients;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.AnalyzeGamePayload;
import protocol.payload.AnalyzeResultPayload;
import protocol.payload.ChangePasswordPayload;
import protocol.payload.GamesPayload;
import protocol.payload.ResponsePayload;
import protocol.payload.SaveGamePayload;
import protocol.payload.UserInfoPayload;
import websocket.ServerSocket;
import websocket.WebSocket;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GatewayMessageRouter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GatewayMessageRouter.class);

    private final ServiceClients clients;
    private final ServerSocket serverSocket;
    private final WebSocket webSocket;
    private final AuthController authController;
    private final JwtService jwtService;
    private final ExecutorService executor;

    public GatewayMessageRouter() {
        clients = new ServiceClients();
        jwtService = new JwtService();

        try {
            authController = new AuthController(clients, jwtService);
            authController.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start auth server", e);
        }

        webSocket = new WebSocket();
        WebSocket.setLogic(this);
        WebSocket.setJwtService(jwtService);
        serverSocket = new ServerSocket("0.0.0.0", 8080, "/", null, WebSocket.class);
        serverSocket.start();
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void processWebSocketMessage(ChessMessage message, String user) {
        log.info("[WS] Processing {} from {}", message.type(), user);

        executor.submit(() -> {
            try {
                handleMessage(message, user);
            } catch (StatusRuntimeException e) {
                handleGrpcError(user, message.type(), e);
            } catch (Exception e) {
                log.error("[WS] Error processing {} for {}: {}", message.type(), user, e.getMessage(), e);
                webSocket.sendToUser(user, ChessMessage.error("INTERNAL_ERROR", "Request failed: " + e.getMessage()));
            }
        });
    }

    private void handleMessage(ChessMessage message, String user) {
        switch (message.type()) {
            case USER_INFO_REQUEST -> {
                UserInfoResponse response = clients.userService().getUserInfo(
                    UserInfoRequest.newBuilder().setUser(user).build());
                UserInfoPayload payload = new UserInfoPayload(
                    response.getEmail(), response.getTotalGames(), response.getCheatGames(), response.getLegalGames());
                webSocket.sendToUser(user, new ChessMessage(MessageType.USER_INFO, payload));
            }
            case GAMES_REQUEST -> {
                GetGamesResponse response = clients.gameService().getGames(
                    GetGamesRequest.newBuilder().setUser(user).build());
                List<GamesPayload.GameEntry> games = response.getGamesList().stream()
                    .map(game -> new GamesPayload.GameEntry(game.getMoves(), game.getLegal()))
                    .toList();
                webSocket.sendToUser(user, new ChessMessage(MessageType.GAMES, new GamesPayload(games)));
            }
            case CHANGE_PASSWORD -> {
                ChangePasswordPayload payload = (ChangePasswordPayload) message.payload();
                ChangePasswordResponse response = clients.userService().changePassword(
                    ChangePasswordRequest.newBuilder().setUser(user).setPassword(payload.password()).build());
                webSocket.sendToUser(user, new ChessMessage(MessageType.CHANGE_PASSWORD_RESPONSE,
                    new ResponsePayload(response.getSuccess(), response.getSuccess() ? "OK" : "KO")));
            }
            case ANALYZE_GAME -> {
                AnalyzeGamePayload payload = (AnalyzeGamePayload) message.payload();
                AnalyzeGameResponse response = clients.analysisService().analyzeGame(
                    AnalyzeGameRequest.newBuilder().setUser(user).setMoves(payload.moves()).build());
                AnalyzeResultPayload resultPayload = new AnalyzeResultPayload(
                    response.getLegal(), response.getWhiteList(), response.getBlackList());
                webSocket.sendToUser(user, new ChessMessage(MessageType.ANALYZE_RESULT, resultPayload));
            }
            case SAVE_GAME -> {
                SaveGamePayload payload = (SaveGamePayload) message.payload();
                SaveGameResponse response = clients.gameService().saveGame(
                    SaveGameRequest.newBuilder()
                        .setUser(user)
                        .setMoves(payload.moves())
                        .setLegal(payload.legal())
                        .build());
                webSocket.sendToUser(user, new ChessMessage(MessageType.SAVE_GAME_RESPONSE,
                    new ResponsePayload(response.getSuccess(), response.getMessage())));
            }
            default -> {
                log.error("[WS] Unhandled or unauthorized message type: {}", message.type());
                webSocket.sendToUser(user, ChessMessage.error("UNAUTHORIZED_TYPE",
                    "Message type " + message.type() + " is not allowed over WebSocket"));
            }
        }
    }

    private void handleGrpcError(String user, MessageType type, StatusRuntimeException e) {
        String errorCode = grpcErrorCode(e.getStatus().getCode());
        String message = grpcErrorMessage(e, type);
        log.error("[WS] gRPC error processing {} for {}: {}", type, user, message, e);
        webSocket.sendToUser(user, ChessMessage.error(errorCode, message));
    }

    private String grpcErrorCode(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT -> "INVALID_ARGUMENT";
            case NOT_FOUND -> "NOT_FOUND";
            case ALREADY_EXISTS -> "ALREADY_EXISTS";
            case PERMISSION_DENIED -> "PERMISSION_DENIED";
            case UNAUTHENTICATED -> "UNAUTHENTICATED";
            case FAILED_PRECONDITION -> "FAILED_PRECONDITION";
            case DEADLINE_EXCEEDED -> "SERVICE_TIMEOUT";
            case UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> "INTERNAL_ERROR";
        };
    }

    private String grpcErrorMessage(StatusRuntimeException e, MessageType type) {
        String description = e.getStatus().getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }

        return switch (e.getStatus().getCode()) {
            case DEADLINE_EXCEEDED -> "Service timeout while processing " + type;
            case UNAVAILABLE -> "Requested service is unavailable";
            case INVALID_ARGUMENT -> "Invalid request";
            case NOT_FOUND -> "Requested resource was not found";
            case PERMISSION_DENIED, UNAUTHENTICATED -> "Request is not authorized";
            default -> "Request failed";
        };
    }
}
