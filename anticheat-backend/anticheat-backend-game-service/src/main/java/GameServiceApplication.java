import logic.GameRequestProcessor;
import utils.HealthServer;

public class GameServiceApplication {
    private static final String MQTT_CONFIG_PATH = "chessfraud-libs/protocol/src/main/configuration/game-service/mqtt.properties";
    private static final int HEALTH_PORT = 9092;

    public void startModule() {
        HealthServer.start(HEALTH_PORT);
        new GameRequestProcessor(MQTT_CONFIG_PATH);
    }

    public static void main(String[] args) {
        new GameServiceApplication().startModule();
    }
}
