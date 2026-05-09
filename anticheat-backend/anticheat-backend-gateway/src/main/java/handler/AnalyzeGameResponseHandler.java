package handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.AnalyzeResultPayload;
import utils.JsonUtils;
import websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;

/** Routes an {@code AnalyzeGameResponse} MQTT message back to the requesting WebSocket client. */
public class AnalyzeGameResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeGameResponseHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebSocket webSocket;

    public AnalyzeGameResponseHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String getHandledMessageType() { return "AnalyzeGameResponse"; }

    @Override
    public void handle(String jsonPayload) {
        String user = JsonUtils.extractField(jsonPayload, "user");
        try {
            JsonNode root  = MAPPER.readTree(jsonPayload);
            boolean legal  = root.get("legal").asBoolean();
            List<Integer> white = new ArrayList<>();
            List<Integer> black = new ArrayList<>();
            for (JsonNode n : root.get("white")) white.add(n.asInt());
            for (JsonNode n : root.get("black")) black.add(n.asInt());
            webSocket.sendToUser(user, new ChessMessage(MessageType.ANALYZE_RESULT,
                new AnalyzeResultPayload(legal, white, black)));
        } catch (Exception e) {
            log.error("[AnalyzeGameResponseHandler] Failed to parse AnalyzeGameResponse: {}", e.getMessage(), e);
        }
    }
}
