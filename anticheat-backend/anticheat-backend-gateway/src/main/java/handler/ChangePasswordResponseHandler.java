package handler;

import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.ResponsePayload;
import utils.JsonUtils;
import websocket.WebSocket;

/**
 * Routes a {@code ChangePasswordResponse} MQTT message back to the requesting WebSocket client.
 * This allows the frontend to receive confirmation when a password change (by logged-in user) succeeds or fails.
 */
public class ChangePasswordResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordResponseHandler.class);

    private final WebSocket webSocket;

    public ChangePasswordResponseHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String getHandledMessageType() { return "ChangePasswordResponse"; }

    @Override
    public void handle(String jsonPayload) {
        String user = JsonUtils.extractField(jsonPayload, "user");
        String isChanged = JsonUtils.extractField(jsonPayload, "isChanged");
        boolean success = "true".equalsIgnoreCase(isChanged);

        String message = success ? "Password changed successfully" : "Failed to change password";
        webSocket.sendToUser(user, new ChessMessage(MessageType.CHANGE_PASSWORD_RESPONSE,
            new ResponsePayload(success, message)));
        log.info("[MQTT] ChangePasswordResponse for user '{}': {}", user, success);
    }
}
