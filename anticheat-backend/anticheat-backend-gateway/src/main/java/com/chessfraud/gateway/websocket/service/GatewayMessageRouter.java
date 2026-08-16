package com.chessfraud.gateway.websocket.service;

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
import com.chessfraud.gateway.grpc.ServiceClients;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import com.chessfraud.protocol.ChessMessage;
import com.chessfraud.protocol.MessageType;
import com.chessfraud.protocol.payload.AnalyzeGamePayload;
import com.chessfraud.protocol.payload.AnalyzeResultPayload;
import com.chessfraud.protocol.payload.ChangePasswordPayload;
import com.chessfraud.protocol.payload.GamesPayload;
import com.chessfraud.protocol.payload.ResponsePayload;
import com.chessfraud.protocol.payload.SaveGamePayload;
import com.chessfraud.protocol.payload.UserInfoPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Translates an inbound {@link ChessMessage} from an authenticated WebSocket session into the
 * corresponding gRPC call, and always returns a response message for that same user (never
 * throws) - the caller is responsible for writing it back to the originating session.
 */
@Service
public class GatewayMessageRouter {
    private static final Logger log = LoggerFactory.getLogger(GatewayMessageRouter.class);

    private final ServiceClients clients;

    public GatewayMessageRouter(ServiceClients clients) {
        this.clients = clients;
    }

    public ChessMessage route(ChessMessage message, String user) {
        log.info("[WS] Processing {} from {}", message.type(), user);
        try {
            return handleMessage(message, user);
        } catch (StatusRuntimeException e) {
            return handleGrpcError(user, message.type(), e);
        } catch (Exception e) {
            log.error("[WS] Error processing {} for {}: {}", message.type(), user, e.getMessage(), e);
            return ChessMessage.error("INTERNAL_ERROR", "Request failed: " + e.getMessage());
        }
    }

    private ChessMessage handleMessage(ChessMessage message, String user) {
        return switch (message.type()) {
            case USER_INFO_REQUEST -> {
                UserInfoResponse response = clients.userService().getUserInfo(
                    UserInfoRequest.newBuilder().setUser(user).build());
                UserInfoPayload payload = new UserInfoPayload(
                    response.getEmail(), response.getTotalGames(), response.getCheatGames(), response.getLegalGames());
                yield new ChessMessage(MessageType.USER_INFO, payload);
            }
            case GAMES_REQUEST -> {
                GetGamesResponse response = clients.gameService().getGames(
                    GetGamesRequest.newBuilder().setUser(user).build());
                List<GamesPayload.GameEntry> games = response.getGamesList().stream()
                    .map(game -> new GamesPayload.GameEntry(game.getMoves(), game.getLegal()))
                    .toList();
                yield new ChessMessage(MessageType.GAMES, new GamesPayload(games));
            }
            case CHANGE_PASSWORD -> {
                ChangePasswordPayload payload = (ChangePasswordPayload) message.payload();
                ChangePasswordResponse response = clients.userService().changePassword(
                    ChangePasswordRequest.newBuilder().setUser(user).setPassword(payload.password()).build());
                yield new ChessMessage(MessageType.CHANGE_PASSWORD_RESPONSE,
                    new ResponsePayload(response.getSuccess(), response.getSuccess() ? "OK" : "KO"));
            }
            case ANALYZE_GAME -> {
                AnalyzeGamePayload payload = (AnalyzeGamePayload) message.payload();
                AnalyzeGameResponse response = clients.analysisService().analyzeGame(
                    AnalyzeGameRequest.newBuilder().setUser(user).setMoves(payload.moves()).build());
                AnalyzeResultPayload resultPayload = new AnalyzeResultPayload(
                    response.getWhiteLegal(), response.getBlackLegal(),
                    response.getWhiteList(), response.getBlackList());
                yield new ChessMessage(MessageType.ANALYZE_RESULT, resultPayload);
            }
            case SAVE_GAME -> {
                SaveGamePayload payload = (SaveGamePayload) message.payload();
                SaveGameResponse response = clients.gameService().saveGame(
                    SaveGameRequest.newBuilder()
                        .setUser(user)
                        .setMoves(payload.moves())
                        .setLegal(payload.legal())
                        .build());
                yield new ChessMessage(MessageType.SAVE_GAME_RESPONSE,
                    new ResponsePayload(response.getSuccess(), response.getMessage()));
            }
            default -> {
                log.error("[WS] Unhandled or unauthorized message type: {}", message.type());
                yield ChessMessage.error("UNAUTHORIZED_TYPE",
                    "Message type " + message.type() + " is not allowed over WebSocket");
            }
        };
    }

    private ChessMessage handleGrpcError(String user, MessageType type, StatusRuntimeException e) {
        String errorCode = grpcErrorCode(e.getStatus().getCode());
        String message = grpcErrorMessage(e, type);
        log.error("[WS] gRPC error processing {} for {}: {}", type, user, message, e);
        return ChessMessage.error(errorCode, message);
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
