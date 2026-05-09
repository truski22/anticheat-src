package handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.UserInfoPayload;
import utils.JsonUtils;
import websocket.WebSocket;

/** Routes a {@code UserInfo} MQTT message back to the requesting WebSocket client. */
public class UserInfoResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(UserInfoResponseHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebSocket webSocket;

    public UserInfoResponseHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String getHandledMessageType() { return "UserInfo"; }

    @Override
    public void handle(String jsonPayload) {
        String user = JsonUtils.extractField(jsonPayload, "user");
        try {
            JsonNode root = MAPPER.readTree(jsonPayload);
            UserInfoPayload payload = new UserInfoPayload(
                root.get("email").asText(),
                root.get("totalGames").asInt(),
                root.get("cheatGames").asInt(),
                root.get("legalGames").asInt()
            );
            webSocket.sendToUser(user, new ChessMessage(MessageType.USER_INFO, payload));
        } catch (Exception e) {
            log.error("[UserInfoResponseHandler] Failed to parse UserInfo payload: {}", e.getMessage(), e);
        }
    }
}
