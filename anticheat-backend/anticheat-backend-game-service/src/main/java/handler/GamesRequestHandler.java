package handler;

import mqtt.MqttPayloadHandler;

import mqtt.Game;
import mqtt.Games;
import mqtt.MqttInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.GamePersistenceService;
import service.GamesResult;
import utils.JsonUtils;

/**
 * Handles {@code GamesRequest} MQTT messages.
 * Retrieves a user's game history from {@link GamePersistenceService} and publishes a
 * {@link Games} response.
 */
public class GamesRequestHandler implements MqttPayloadHandler {

    private static final Logger log = LoggerFactory.getLogger(GamesRequestHandler.class);

    private final GamePersistenceService gameService;
    private final MqttInterface mqttInterface;

    /**
     * @param gameService   service that retrieves game records from the database
     * @param mqttInterface used to publish the response message
     */
    public GamesRequestHandler(GamePersistenceService gameService, MqttInterface mqttInterface) {
        this.gameService    = gameService;
        this.mqttInterface  = mqttInterface;
    }

    /**
     * Parses the incoming MQTT JSON payload, fetches the user's games, and publishes a
     * {@link Games} message.
     * <p>Does nothing if any required field ({@code user}, {@code idMessage}) is missing.</p>
     *
     * @param mqttMessage raw JSON string received from the MQTT broker
     */
    @Override
    public String getHandledMessageType() { return "GamesRequest"; }

    @Override

    public void handle(String mqttMessage) {
        String user      = JsonUtils.extractField(mqttMessage, "user");
        String idMessage = JsonUtils.extractField(mqttMessage, "idMessage");

        if (user == null || idMessage == null) {
            log.warn("[GamesRequestHandler] Missing required fields in GamesRequest message");
            return;
        }

        GamesResult result = gameService.getGames(user);

        Games games = new Games();
        games.setIdMessage(idMessage);
        games.setMessageType("Games");
        games.setUser(user);
        for (Game game : result.getGames()) {
            games.addGame(game);
        }

        mqttInterface.publishMessage(games);
    }
}


