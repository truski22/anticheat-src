package logic;

import config.GameServiceConfig;
import handler.GamesRequestHandler;
import handler.SaveGameRequestHandler;
import mqtt.MqttInterface;
import mqtt.MqttMessageRouter;
import mqtt.exception.MqttException;
import service.GamePersistenceService;
import utils.Database;

import java.util.List;

public class GameRequestProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameRequestProcessor.class);

    private final MqttInterface mqttInterface;
    private final MqttMessageRouter router;
    private final Database database;

    public GameRequestProcessor(String path) {
        try {
            mqttInterface = new MqttInterface(path);
            database      = new Database();

            GamePersistenceService gameService = new GamePersistenceService(database);

            router = mqttInterface.createRouter(List.of(
                new SaveGameRequestHandler(gameService, mqttInterface),
                new GamesRequestHandler(gameService, mqttInterface)
            ));
            router.start();
        } catch (MqttException e) {
            log.error("Failed to initialize GameRequestProcessor: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize GameRequestProcessor", e);
        }
    }

    public void stop() {
        router.stopRouter();
        if (mqttInterface != null) mqttInterface.stopInterface();
        if (database      != null) database.close();
    }
}
