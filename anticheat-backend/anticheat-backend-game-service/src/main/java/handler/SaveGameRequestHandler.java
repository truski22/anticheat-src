package handler;

import mqtt.MqttPayloadHandler;

import com.fasterxml.jackson.databind.JsonNode;
import mqtt.MqttInterface;
import mqtt.SaveGameResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.GamePersistenceService;
import service.SaveGameResult;
import utils.JsonUtils;

/**
 * Handles {@code SaveGame} MQTT messages.
 * Delegates persistence to {@link GamePersistenceService} and publishes a
 * {@link SaveGameResponse} with result code {@code "OK"} on success.
 */
public class SaveGameRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(SaveGameRequestHandler.class);

    private final GamePersistenceService gameService;
    private final MqttInterface mqttInterface;

    /**
     * @param gameService   service that persists the game record
     * @param mqttInterface used to publish the response message
     */
    public SaveGameRequestHandler(GamePersistenceService gameService, MqttInterface mqttInterface) {
        this.gameService    = gameService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, persists the game, and publishes a
     * {@link SaveGameResponse}.
     * <p>Does nothing if any required field ({@code idMessage}, {@code user},
     * or the nested {@code game.moves}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "SaveGame"; }

    @Override

    public void handle(String mqttMessage) {
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");
        String username  = JsonUtils.extractField(mqttMessage, "user");
        JsonNode gameNode = JsonUtils.extractNode(mqttMessage, "game");

        String moves = gameNode != null && gameNode.has("moves") ? gameNode.get("moves").asText() : null;
        boolean legal = gameNode != null && gameNode.has("legal") && gameNode.get("legal").asBoolean();

        if (idMessage == null || username == null || moves == null) {
            log.warn("[SaveGameRequestHandler] Missing required fields in SaveGame message");
            return;
        }

        SaveGameResult result = gameService.saveGame(username, moves, legal);

        SaveGameResponse response = new SaveGameResponse();
        response.setIdMessage(idMessage);
        response.setMessageType("SaveGameResponse");
        response.setUser(username);
        if (result.isSuccess()) {
            response.setResponse("OK");
        } else {
            log.error("[SaveGameRequestHandler] Failed to save game for user '{}'", username);
        }

        mqttInterface.publishMessage(response);
    }
}


