package handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import mqtt.Games;
import mqtt.MqttPayloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.ChessMessage;
import protocol.MessageType;
import protocol.payload.GamesPayload;
import utils.JsonUtils;
import websocket.WebSocket;

import java.util.List;

/** Routes a {@code Games} MQTT message back to the requesting WebSocket client. */
public class GamesResponseHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(GamesResponseHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebSocket webSocket;

    public GamesResponseHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String getHandledMessageType() { return "Games"; }

    @Override
    public void handle(String jsonPayload) {
        String user = JsonUtils.extractField(jsonPayload, "user");
        try {
            Games games = MAPPER.readValue(jsonPayload, Games.class);
            List<GamesPayload.GameEntry> entries = games.getGames().stream()
                .map(g -> new GamesPayload.GameEntry(g.getMoves(), g.isLegal()))
                .toList();
            webSocket.sendToUser(user, new ChessMessage(MessageType.GAMES, new GamesPayload(entries)));
        } catch (Exception e) {
            log.error("[GamesResponseHandler] Failed to parse Games payload: {}", e.getMessage(), e);
        }
    }
}
