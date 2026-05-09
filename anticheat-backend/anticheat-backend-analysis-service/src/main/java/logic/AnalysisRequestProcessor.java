package logic;

import config.AnalysisServiceConfig;
import handler.AnalyzeGameRequestHandler;
import mqtt.MqttInterface;
import mqtt.MqttMessageRouter;
import mqtt.exception.MqttException;
import service.GameAnalysisService;

import java.util.List;

public class AnalysisRequestProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalysisRequestProcessor.class);

    private final MqttInterface mqttInterface;
    private final MqttMessageRouter router;

    public AnalysisRequestProcessor(String path) {
        try {
            mqttInterface = new MqttInterface(path);

            AnalysisServiceConfig config      = AnalysisServiceConfig.fromEnvironment();
            GameAnalysisService   analysisService = new GameAnalysisService(config);

            router = mqttInterface.createRouter(List.of(
                new AnalyzeGameRequestHandler(analysisService, mqttInterface)
            ));
            router.start();
        } catch (MqttException e) {
            log.error("Failed to initialize AnalysisRequestProcessor: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize AnalysisRequestProcessor", e);
        }
    }

    public void stop() {
        router.stopRouter();
        if (mqttInterface != null) mqttInterface.stopInterface();
    }
}
