import logic.AnalysisRequestProcessor;
import utils.HealthServer;

public class AnalysisServiceApplication {
    private static final String MQTT_CONFIG_PATH = "chessfraud-libs/protocol/src/main/configuration/analysis-service/mqtt.properties";
    private static final int HEALTH_PORT = 9093;

    public void startModule() {
        HealthServer.start(HEALTH_PORT);
        new AnalysisRequestProcessor(MQTT_CONFIG_PATH);
    }

    public static void main(String[] args) {
        new AnalysisServiceApplication().startModule();
    }
}
