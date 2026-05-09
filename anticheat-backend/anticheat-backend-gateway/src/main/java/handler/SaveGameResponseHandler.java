package handler;

import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.ResponsePayload;
import utils.JsonUtils;
import websocket.WebSocket;

/** Routes a {@code SaveGameResponse} MQTT message back to the requesting WebSocket client. */
public class SaveGameResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(SaveGameResponseHandler.class);

    private final WebSocket webSocket;

    public SaveGameResponseHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String getHandledMessageType() { return "SaveGameResponse"; }

    @Override
    public void handle(String jsonPayload) {
        String user     = JsonUtils.extractField(jsonPayload, "user");
        String response = JsonUtils.extractField(jsonPayload, "response");
        boolean success = "OK".equalsIgnoreCase(response);
        webSocket.sendToUser(user, new ChessMessage(MessageType.SAVE_GAME_RESPONSE,
            new ResponsePayload(success, response)));
    }
}
