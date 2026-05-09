import logic.UserRequestProcessor;
import utils.HealthServer;

public class UserServiceApplication {
    private static final String MQTT_CONFIG_PATH = "chessfraud-libs/protocol/src/main/configuration/user-service/mqtt.properties";
    private static final int HEALTH_PORT = 9091;

    public void startModule() {
        HealthServer.start(HEALTH_PORT);
        new UserRequestProcessor(MQTT_CONFIG_PATH);
    }

    public static void main(String[] args) {
        new UserServiceApplication().startModule();
    }
}
