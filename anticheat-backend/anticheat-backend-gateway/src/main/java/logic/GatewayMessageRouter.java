package logic;

import auth.AuthController;
import auth.JwtService;
import handler.*;
import mqtt.*;
import mqtt.exception.MqttException;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.*;
import websocket.ServerSocket;
import websocket.WebSocket;

import java.io.IOException;
import java.util.List;

public class GatewayMessageRouter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GatewayMessageRouter.class);

    private final MqttInterface mqttInterface;
    private final MqttMessageRouter mqttRouter;
    private final ServerSocket serverSocket;
    private final WebSocket webSocket;
    private final AuthController authController;
    private final JwtService jwtService;

    public GatewayMessageRouter(String path) {
        try {
            mqttInterface = new MqttInterface(path);
            jwtService    = new JwtService();
            authController = new AuthController(mqttInterface, jwtService);
            authController.start();

            webSocket = new WebSocket();
            WebSocket.setLogic(this);
            WebSocket.setJwtService(jwtService);
            serverSocket = new ServerSocket("0.0.0.0", 8080, "/", null, WebSocket.class);
            serverSocket.start();

            mqttRouter = mqttInterface.createRouter(List.of(
                new LoginResponseHandler(webSocket, authController),
                new RegisterResponseHandler(webSocket, authController),
                new UserInfoResponseHandler(webSocket),
                new GamesResponseHandler(webSocket),
                new AnalyzeGameResponseHandler(webSocket),
                new SaveGameResponseHandler(webSocket),
                new ChangePasswordEmailResponseHandler(authController),
                new ChangePasswordResponseHandler(webSocket)
            ));
            mqttRouter.start();

        } catch (MqttException e) {
            throw new RuntimeException("Failed to start gateway", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start auth server", e);
        }
    }

    // ── WebSocket → MQTT (authenticated requests only) ────────────────────

    public void processWebSocketMessage(ChessMessage message, String user) {
        log.info("[WS] Processing {} from {}", message.type(), user);

        switch (message.type()) {
            case USER_INFO_REQUEST -> {
                UserInfoRequest req = new UserInfoRequest();
                req.setUser(user);
                req.setMessageType("UserInfoRequest");
                mqttInterface.publishMessage(req);
            }
            case GAMES_REQUEST -> {
                GamesRequest req = new GamesRequest();
                req.setUser(user);
                req.setMessageType("GamesRequest");
                mqttInterface.publishMessage(req);
            }
            case CHANGE_PASSWORD -> {
                ChangePasswordPayload p = (ChangePasswordPayload) message.payload();
                ChangePassword req = new ChangePassword();
                req.setUser(user);
                req.setPassword(p.password());
                req.setMessageType("ChangePassword");
                mqttInterface.publishMessage(req);
            }
            case ANALYZE_GAME -> {
                AnalyzeGamePayload p = (AnalyzeGamePayload) message.payload();
                AnalyzeGame req = new AnalyzeGame();
                req.setUser(user);
                req.setMoves(p.moves());
                req.setMessageType("AnalyzeGame");
                mqttInterface.publishMessage(req);
            }
            case SAVE_GAME -> {
                SaveGamePayload p = (SaveGamePayload) message.payload();
                SaveGame req = new SaveGame();
                Game game = new Game();
                game.setMoves(p.moves());
                game.setLegal(p.legal());
                req.setGame(game);
                req.setUser(user);
                req.setMessageType("SaveGame");
                mqttInterface.publishMessage(req);
            }
            default -> {
                log.error("[WS] Unhandled or unauthorized message type: {}", message.type());
                webSocket.sendToUser(user, ChessMessage.error("UNAUTHORIZED_TYPE",
                    "Message type " + message.type() + " is not allowed over WebSocket"));
            }
        }
    }
}


